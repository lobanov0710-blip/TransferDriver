package ru.transferservis.driver.data.repository;

import androidx.annotation.NonNull;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import ru.transferservis.driver.data.api.ApiClient;
import ru.transferservis.driver.data.api.StaffApi;
import ru.transferservis.driver.data.model.BasicResponse;
import ru.transferservis.driver.data.model.LoginRequest;
import ru.transferservis.driver.data.model.StaffLoginResponse;
import ru.transferservis.driver.data.model.StaffRefreshTokenRequest;

public class StaffRepository {

    public interface LoginCallback {

        void onSuccess(
                StaffLoginResponse response
        );

        void onError(
                int httpStatus,
                String message
        );
    }

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

    public void login(
            String login,
            String password,
            LoginCallback callback
    ) {

        if (callback == null) {
            return;
        }

        String normalizedLogin =
                login == null
                        ? ""
                        : login
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (
                normalizedLogin.isEmpty()
        ) {

            callback.onError(
                    0,
                    "Введите логин"
            );

            return;
        }

        if (
                password == null
                        ||
                        password.isEmpty()
        ) {

            callback.onError(
                    0,
                    "Введите пароль"
            );

            return;
        }

        LoginRequest request =
                new LoginRequest(
                        normalizedLogin,
                        password
                );

        staffApi
                .login(
                        request
                )
                .enqueue(
                        new Callback<StaffLoginResponse>() {

                            @Override
                            public void onResponse(
                                    @NonNull Call<StaffLoginResponse> call,
                                    @NonNull Response<StaffLoginResponse> response
                            ) {

                                if (
                                        !response.isSuccessful()
                                ) {

                                    callback.onError(
                                            response.code(),
                                            mapLoginHttpError(
                                                    response.code()
                                            )
                                    );

                                    return;
                                }

                                StaffLoginResponse body =
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

                                callback.onSuccess(
                                        body
                                );
                            }

                            @Override
                            public void onFailure(
                                    @NonNull Call<StaffLoginResponse> call,
                                    @NonNull Throwable throwable
                            ) {

                                if (
                                        call.isCanceled()
                                ) {

                                    return;
                                }

                                callback.onError(
                                        0,
                                        "Не удалось подключиться к серверу"
                                );
                            }
                        }
                );
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

    private String mapLoginHttpError(
            int status
    ) {

        if (status == 400) {

            return "Проверьте введённые данные";
        }

        if (status == 401) {

            return "Неверный логин или пароль";
        }

        if (status == 403) {

            return "Доступ запрещён";
        }

        if (status >= 500) {

            return "Сервер временно недоступен";
        }

        return "Ошибка сервера";
    }
}