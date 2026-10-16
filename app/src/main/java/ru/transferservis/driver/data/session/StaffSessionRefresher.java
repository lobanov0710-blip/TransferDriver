package ru.transferservis.driver.data.session;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.transferservis.driver.data.api.StaffTokenRefreshClient;
import ru.transferservis.driver.data.model.StaffDto;
import ru.transferservis.driver.data.model.StaffLoginResponse;

public final class StaffSessionRefresher {

    public enum ResultType {
        READY,
        AUTHENTICATION_REQUIRED,
        TEMPORARY_FAILURE
    }

    public static final class Result {

        private final ResultType type;

        private final DriverSession session;

        private Result(
                ResultType type,
                DriverSession session
        ) {

            this.type =
                    type;

            this.session =
                    session;
        }

        public static Result ready(
                DriverSession session
        ) {

            return new Result(
                    ResultType.READY,
                    session
            );
        }

        public static Result authenticationRequired() {

            return new Result(
                    ResultType.AUTHENTICATION_REQUIRED,
                    null
            );
        }

        public static Result temporaryFailure() {

            return new Result(
                    ResultType.TEMPORARY_FAILURE,
                    null
            );
        }

        public ResultType getType() {

            return type;
        }

        public DriverSession getSession() {

            return session;
        }
    }

    public interface Callback {

        void onResult(
                Result result
        );
    }

    /*
     * Все refresh rotation внутри процесса
     * приложения проходят через один lock.
     *
     * Это критично, потому что повторное
     * использование уже ротированного
     * refresh token backend расценивает
     * как replay.
     */
    private static final Object REFRESH_LOCK =
            new Object();

    private static final ExecutorService REFRESH_EXECUTOR =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(
                    Looper.getMainLooper()
            );

    private final SecureSessionStore sessionStore;

    private final StaffTokenRefreshClient refreshClient;

    public StaffSessionRefresher(
            SecureSessionStore sessionStore
    ) {

        this.sessionStore =
                sessionStore;

        refreshClient =
                new StaffTokenRefreshClient();
    }

    public void refreshExpiredSessionAsync(
            Callback callback
    ) {

        if (callback == null) {
            return;
        }

        REFRESH_EXECUTOR.execute(
                () -> {

                    Result result =
                            refreshExpiredSessionBlocking();

                    mainHandler.post(
                            () ->
                                    callback.onResult(
                                            result
                                    )
                    );
                }
        );
    }

    public Result refreshExpiredSessionBlocking() {

        synchronized (
                REFRESH_LOCK
        ) {

            DriverSession current =
                    sessionStore.load();

            if (
                    !hasRefreshableSession(
                            current
                    )
            ) {

                sessionStore.clear();

                return Result
                        .authenticationRequired();
            }

            /*
             * Пока поток ждал lock,
             * другой запрос мог уже
             * успешно обновить session.
             */
            if (
                    current
                            .hasUsableAccessToken()
            ) {

                return Result.ready(
                        current
                );
            }

            return rotateLocked(
                    current
            );
        }
    }

    public Result refreshAfterUnauthorizedBlocking(
            String accessTokenUsed
    ) {

        synchronized (
                REFRESH_LOCK
        ) {

            DriverSession current =
                    sessionStore.load();

            if (
                    !hasRefreshableSession(
                            current
                    )
            ) {

                sessionStore.clear();

                return Result
                        .authenticationRequired();
            }

            /*
             * Другой запрос уже мог получить 401,
             * выполнить refresh и сохранить
             * новый access token.
             *
             * Тогда второй refresh выполнять
             * категорически нельзя.
             */
            if (
                    current
                            .hasUsableAccessToken()
                            &&
                            accessTokenUsed != null
                            &&
                            !accessTokenUsed.equals(
                                    current.getAccessToken()
                            )
            ) {

                return Result.ready(
                        current
                );
            }

            return rotateLocked(
                    current
            );
        }
    }

    private Result rotateLocked(
            DriverSession current
    ) {

        StaffTokenRefreshClient.Result
                transportResult =
                refreshClient
                        .refreshBlocking(
                                current.getRefreshToken()
                        );

        switch (
                transportResult.getType()
        ) {

            case TEMPORARY_FAILURE:

                return Result
                        .temporaryFailure();

            case AUTHENTICATION_REQUIRED:

                sessionStore.clear();

                return Result
                        .authenticationRequired();

            case SUCCESS:
                break;
        }

        StaffLoginResponse response =
                transportResult.getResponse();

        if (
                !sameDriverIdentity(
                        current,
                        response
                )
        ) {

            sessionStore.clear();

            return Result
                    .authenticationRequired();
        }

        if (
                !sessionStore.save(
                        response
                )
        ) {

            /*
             * Сервер уже мог ротировать
             * refresh token.
             *
             * Если новый token локально
             * сохранить не удалось,
             * старый повторять нельзя.
             */
            sessionStore.clear();

            return Result
                    .authenticationRequired();
        }

        DriverSession refreshed =
                sessionStore.load();

        if (
                refreshed == null
                        ||
                        !refreshed.isDriver()
                        ||
                        !refreshed
                                .hasUsableAccessToken()
                        ||
                        !refreshed
                                .hasUsableRefreshToken()
        ) {

            sessionStore.clear();

            return Result
                    .authenticationRequired();
        }

        return Result.ready(
                refreshed
        );
    }

    private boolean hasRefreshableSession(
            DriverSession session
    ) {

        return session != null
                &&
                session.isDriver()
                &&
                session
                        .hasUsableRefreshToken();
    }

    private boolean sameDriverIdentity(
            DriverSession current,
            StaffLoginResponse response
    ) {

        if (
                current == null
                        ||
                        response == null
                        ||
                        !response.isOk()
        ) {

            return false;
        }

        StaffDto staff =
                response.getStaff();

        if (staff == null) {
            return false;
        }

        String currentId =
                current.getStaffId();

        String refreshedId =
                staff.getId();

        if (
                currentId == null
                        ||
                        refreshedId == null
                        ||
                        !currentId.equals(
                                refreshedId
                        )
        ) {

            return false;
        }

        return staff.getRole() != null
                &&
                "driver".equalsIgnoreCase(
                        staff
                                .getRole()
                                .trim()
                );
    }
}