package ru.transferservis.driver.data.repository;

import androidx.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import ru.transferservis.driver.data.api.ApiClient;
import ru.transferservis.driver.data.api.StaffApi;
import ru.transferservis.driver.data.model.DriverFirebaseSessionExchangeRequest;
import ru.transferservis.driver.data.model.StaffDto;
import ru.transferservis.driver.data.model.StaffLoginResponse;
import ru.transferservis.driver.security.UuidValidator;

public final class DriverFirebaseSessionRepository {

    public interface SessionCallback {

        void onSuccess(
                StaffLoginResponse response
        );

        void onIdentityVerificationRequired();

        void onRateLimited();

        void onTemporaryError(
                String message
        );
    }

    private final StaffApi api;

    public DriverFirebaseSessionRepository() {

        api =
                ApiClient.getStaffApi();
    }

    public void exchange(
            String firebaseIdToken,
            String appCheckToken,
            String installationId,
            SessionCallback callback
    ) {

        if (callback == null) {
            return;
        }

        String safeIdToken =
                clean(
                        firebaseIdToken
                );

        String safeAppCheckToken =
                clean(
                        appCheckToken
                );

        String safeInstallationId =
                clean(
                        installationId
                )
                        .toLowerCase();

        if (
                safeIdToken.isEmpty()
                        ||
                        safeAppCheckToken.isEmpty()
                        ||
                        !UuidValidator.isCanonical(
                                safeInstallationId
                        )
        ) {

            callback
                    .onIdentityVerificationRequired();

            return;
        }

        DriverFirebaseSessionExchangeRequest request =
                new DriverFirebaseSessionExchangeRequest(
                        safeIdToken,
                        safeInstallationId
                );

        api
                .exchangeDriverFirebaseSession(
                        safeAppCheckToken,
                        request
                )
                .enqueue(
                        new Callback<StaffLoginResponse>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<StaffLoginResponse> call,
                                    @NonNull Response<StaffLoginResponse> response
                            ) {

                                int status =
                                        response.code();

                                if (
                                        status == 400
                                                ||
                                                status == 401
                                                ||
                                                status == 403
                                                ||
                                                status == 409
                                ) {

                                    callback
                                            .onIdentityVerificationRequired();

                                    return;
                                }

                                if (status == 429) {

                                    callback
                                            .onRateLimited();

                                    return;
                                }

                                if (
                                        !response.isSuccessful()
                                ) {

                                    callback.onTemporaryError(
                                            mapHttpError(
                                                    status
                                            )
                                    );

                                    return;
                                }

                                StaffLoginResponse body =
                                        response.body();

                                if (
                                        !isValidDriverSession(
                                                body
                                        )
                                ) {

                                    callback.onTemporaryError(
                                            "Некорректный ответ сервера"
                                    );

                                    return;
                                }

                                callback.onSuccess(
                                        body
                                );
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<StaffLoginResponse> call,
                                    @NonNull Throwable throwable
                            ) {

                                if (call.isCanceled()) {
                                    return;
                                }

                                callback.onTemporaryError(
                                        "Не удалось подтвердить вход. Проверьте интернет"
                                );
                            }
                        }
                );
    }

    private boolean isValidDriverSession(
            StaffLoginResponse response
    ) {

        if (
                response == null
                        ||
                        !response.isOk()
        ) {

            return false;
        }

        StaffDto staff =
                response.getStaff();

        if (
                staff == null
                        ||
                        isBlank(
                                staff.getId()
                        )
                        ||
                        isBlank(
                                staff.getRole()
                        )
                        ||
                        !"driver".equalsIgnoreCase(
                                staff.getRole()
                                        .trim()
                        )
        ) {

            return false;
        }

        return !isBlank(
                response.getAccessToken()
        )
                &&
                response.getAccessExpiresAt() > 0
                &&
                !isBlank(
                        response.getRefreshToken()
                )
                &&
                response.getRefreshExpiresAt() > 0;
    }

    private String mapHttpError(
            int status
    ) {

        if (status >= 500) {

            return "Сервис авторизации временно недоступен";
        }

        return "Не удалось подтвердить вход";
    }

    private boolean isBlank(
            String value
    ) {

        return clean(
                value
        ).isEmpty();
    }

    private static String clean(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
