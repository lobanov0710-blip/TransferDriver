package ru.transferservis.driver.data.repository;

import androidx.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import ru.transferservis.driver.data.api.ApiClient;
import ru.transferservis.driver.data.api.StaffApi;
import ru.transferservis.driver.data.model.BasicResponse;
import ru.transferservis.driver.data.model.StaffRefreshTokenRequest;

public final class StaffRepository {

    public interface LogoutCallback {

        void onSuccess();

        void onError(
                int httpStatus,
                String message
        );
    }

    private final StaffApi staffApi;

    public StaffRepository() {

        staffApi =
                ApiClient.getStaffApi();
    }

    public void logout(
            String refreshToken,
            LogoutCallback callback
    ) {

        if (callback == null) {
            return;
        }

        if (
                refreshToken == null
                        ||
                        refreshToken
                                .trim()
                                .isEmpty()
        ) {

            callback.onError(
                    0,
                    "Сессия недействительна"
            );

            return;
        }

        StaffRefreshTokenRequest request =
                new StaffRefreshTokenRequest(
                        refreshToken
                );

        staffApi
                .logout(
                        request
                )
                .enqueue(
                        new Callback<BasicResponse>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<BasicResponse> call,
                                    @NonNull Response<BasicResponse> response
                            ) {

                                if (
                                        !response.isSuccessful()
                                ) {

                                    callback.onError(
                                            response.code(),
                                            "Не удалось завершить сессию"
                                    );

                                    return;
                                }

                                BasicResponse body =
                                        response.body();

                                if (
                                        body == null
                                                ||
                                                !body.isOk()
                                ) {

                                    callback.onError(
                                            response.code(),
                                            "Некорректный ответ сервера"
                                    );

                                    return;
                                }

                                callback.onSuccess();
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<BasicResponse> call,
                                    @NonNull Throwable throwable
                            ) {

                                if (
                                        call.isCanceled()
                                ) {

                                    return;
                                }

                                callback.onError(
                                        0,
                                        "Не удалось завершить сессию. Проверьте интернет"
                                );
                            }
                        }
                );
    }
}
