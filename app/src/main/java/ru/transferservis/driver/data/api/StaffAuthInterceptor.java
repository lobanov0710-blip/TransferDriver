package ru.transferservis.driver.data.api;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import ru.transferservis.driver.data.session.DriverSession;
import ru.transferservis.driver.data.session.SecureSessionStore;
import ru.transferservis.driver.data.session.StaffSessionRefresher;

public final class StaffAuthInterceptor
        implements Interceptor {

    public static final String AUTH_MARKER_HEADER =
            "X-Staff-Auth";

    public static final String AUTH_RECOVERY_HEADER =
            "X-Transfer-Auth-Recovery";

    public static final String AUTH_RECOVERY_TEMPORARY =
            "temporary";

    private static final String BEARER_PREFIX =
            "Bearer ";

    private final SecureSessionStore sessionStore;

    private final StaffSessionRefresher sessionRefresher;

    public StaffAuthInterceptor(
            SecureSessionStore sessionStore
    ) {

        this.sessionStore =
                sessionStore;

        sessionRefresher =
                new StaffSessionRefresher(
                        sessionStore
                );
    }

    @NonNull
    @Override
    public Response intercept(
            @NonNull Chain chain
    ) throws IOException {

        Request originalRequest =
                chain.request();

        String authMarker =
                originalRequest.header(
                        AUTH_MARKER_HEADER
                );

        boolean requiresStaffAuth =
                "true".equalsIgnoreCase(
                        authMarker
                );

        Request baseRequest =
                originalRequest
                        .newBuilder()
                        .removeHeader(
                                AUTH_MARKER_HEADER
                        )
                        .build();

        /*
         * Публичный запрос.
         *
         * Staff authentication для него
         * не требуется.
         */
        if (!requiresStaffAuth) {

            return chain.proceed(
                    baseRequest
            );
        }

        /*
         * =================================
         * PRE-REQUEST AUTH RECOVERY
         * =================================
         *
         * Private API не должен зависеть
         * от того, вызвал ли экран
         * ensureValidSession().
         *
         * Если access JWT уже истёк или
         * находится внутри safety window,
         * но refresh token ещё действителен,
         * обновляем staff-session ДО
         * исходного API-запроса.
         */

        DriverSession session =
                sessionStore.load();

        if (
                session != null
                        &&
                        session.isDriver()
                        &&
                        session
                                .hasUsableRefreshToken()
                        &&
                        !session
                                .hasUsableAccessToken()
        ) {

            StaffSessionRefresher.Result
                    preflightRefresh =
                    sessionRefresher
                            .refreshExpiredSessionBlocking();

            switch (
                    preflightRefresh.getType()
            ) {

                case READY:

                    session =
                            preflightRefresh
                                    .getSession();

                    break;

                case TEMPORARY_FAILURE:

                    return createLocalUnauthorizedResponse(
                            baseRequest,
                            true
                    );

                case AUTHENTICATION_REQUIRED:

                    return createLocalUnauthorizedResponse(
                            baseRequest,
                            false
                    );
            }
        }

        /*
         * После preflight у нас должна
         * существовать полноценная
         * водительская session с
         * пригодным access JWT.
         */

        if (
                session == null
                        ||
                        !session.isDriver()
                        ||
                        !session
                                .hasUsableAccessToken()
        ) {

            return createLocalUnauthorizedResponse(
                    baseRequest,
                    false
            );
        }

        String accessTokenUsed =
                session.getAccessToken();

        Request authenticatedRequest =
                baseRequest
                        .newBuilder()
                        .header(
                                "Authorization",
                                BEARER_PREFIX
                                        + accessTokenUsed
                        )
                        .build();

        Response response =
                chain.proceed(
                        authenticatedRequest
                );

        /*
         * Если backend не вернул 401,
         * никакого refresh/retry не делаем.
         */

        if (
                response.code()
                        != 401
        ) {

            return response;
        }

        /*
         * =================================
         * POST-401 AUTH RECOVERY
         * =================================
         *
         * Даже пригодный локально JWT
         * может получить 401:
         *
         * - JWT успел истечь;
         * - другой запрос уже обновил session;
         * - backend session была изменена;
         * - access token был отозван.
         *
         * StaffSessionRefresher сериализует
         * refresh rotation и не позволяет
         * двум запросам повторно использовать
         * один refresh token.
         */

        StaffSessionRefresher.Result
                refreshResult =
                sessionRefresher
                        .refreshAfterUnauthorizedBlocking(
                                accessTokenUsed
                        );

        switch (
                refreshResult.getType()
        ) {

            case AUTHENTICATION_REQUIRED:

                /*
                 * Возвращаем исходный 401.
                 *
                 * Repository преобразует его
                 * в authenticationRequired.
                 */
                return response;

            case TEMPORARY_FAILURE:

                /*
                 * Помечаем локально, что
                 * причина — временная ошибка
                 * восстановления session,
                 * а не обязательный logout.
                 */
                return response
                        .newBuilder()
                        .header(
                                AUTH_RECOVERY_HEADER,
                                AUTH_RECOVERY_TEMPORARY
                        )
                        .build();

            case READY:
                break;
        }

        DriverSession refreshedSession =
                refreshResult.getSession();

        if (
                refreshedSession == null
                        ||
                        !refreshedSession.isDriver()
                        ||
                        !refreshedSession
                                .hasUsableAccessToken()
        ) {

            return response;
        }

        String refreshedAccessToken =
                refreshedSession
                        .getAccessToken();

        /*
         * Первый HTTP 401 больше
         * не используется.
         *
         * Закрываем его body перед retry.
         */
        response.close();

        Request retryRequest =
                baseRequest
                        .newBuilder()
                        .header(
                                "Authorization",
                                BEARER_PREFIX
                                        + refreshedAccessToken
                        )
                        .build();

        /*
         * Выполняем только одну
         * повторную попытку.
         *
         * chain.proceed() здесь не вызывает
         * этот application interceptor
         * рекурсивно, поэтому бесконечного
         * auth retry loop не возникает.
         */
        return chain.proceed(
                retryRequest
        );
    }

    private Response createLocalUnauthorizedResponse(
            Request request,
            boolean temporaryFailure
    ) {

        /*
         * Это локальный synthetic HTTP 401.
         *
         * Запрос не отправляется на backend,
         * если приложение уже заранее знает,
         * что пригодной staff-session нет.
         */

        ResponseBody responseBody =
                ResponseBody.create(
                        null,
                        ""
                );

        Response.Builder builder =
                new Response.Builder()
                        .request(
                                request
                        )
                        .protocol(
                                Protocol.HTTP_1_1
                        )
                        .code(
                                401
                        )
                        .message(
                                "Staff authentication unavailable"
                        )
                        .body(
                                responseBody
                        );

        if (temporaryFailure) {

            builder.header(
                    AUTH_RECOVERY_HEADER,
                    AUTH_RECOVERY_TEMPORARY
            );
        }

        return builder.build();
    }
}