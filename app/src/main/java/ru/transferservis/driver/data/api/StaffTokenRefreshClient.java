package ru.transferservis.driver.data.api;

import java.io.IOException;

import okhttp3.OkHttpClient;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import ru.transferservis.driver.data.model.StaffLoginResponse;
import ru.transferservis.driver.data.model.StaffRefreshTokenRequest;

public final class StaffTokenRefreshClient {

    public enum ResultType {
        SUCCESS,
        AUTHENTICATION_REQUIRED,
        TEMPORARY_FAILURE
    }

    public static final class Result {

        private final ResultType type;

        private final StaffLoginResponse response;

        private Result(
                ResultType type,
                StaffLoginResponse response
        ) {

            this.type =
                    type;

            this.response =
                    response;
        }

        public static Result success(
                StaffLoginResponse response
        ) {

            return new Result(
                    ResultType.SUCCESS,
                    response
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

        public StaffLoginResponse getResponse() {

            return response;
        }
    }

    private final StaffApi staffApi;

    public StaffTokenRefreshClient() {

        OkHttpClient okHttpClient =
                new OkHttpClient.Builder()
                        .build();

        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(
                                ApiClient.BASE_URL
                        )
                        .client(
                                okHttpClient
                        )
                        .addConverterFactory(
                                GsonConverterFactory.create()
                        )
                        .build();

        staffApi =
                retrofit.create(
                        StaffApi.class
                );
    }

    public Result refreshBlocking(
            String refreshToken
    ) {

        if (
                refreshToken == null
                        ||
                        refreshToken
                                .trim()
                                .isEmpty()
        ) {

            return Result
                    .authenticationRequired();
        }

        StaffRefreshTokenRequest request =
                new StaffRefreshTokenRequest(
                        refreshToken
                );

        try {

            retrofit2.Response<StaffLoginResponse>
                    response =
                    staffApi
                            .refresh(
                                    request
                            )
                            .execute();

            int status =
                    response.code();

            if (
                    response.isSuccessful()
            ) {

                StaffLoginResponse body =
                        response.body();

                if (
                        body == null
                                ||
                                !body.isOk()
                ) {

                    /*
                     * Неизвестно, было ли состояние
                     * refresh-session изменено.
                     *
                     * Fail closed.
                     */
                    return Result
                            .authenticationRequired();
                }

                return Result.success(
                        body
                );
            }

            if (
                    status == 400
                            ||
                            status == 401
                            ||
                            status == 403
            ) {

                return Result
                        .authenticationRequired();
            }

            /*
             * Сервер явно ответил ошибкой.
             * Текущий refresh token пока
             * не уничтожаем.
             */
            return Result
                    .temporaryFailure();

        }
        catch (
                IOException exception
        ) {

            /*
             * В случае transport uncertainty
             * мы не знаем, успел ли сервер
             * ротировать refresh token.
             *
             * Повтор старого token может быть
             * расценен backend как replay и
             * отозвать всю session-family.
             *
             * Поэтому безопаснее потребовать
             * повторную авторизацию.
             */
            return Result
                    .authenticationRequired();
        }
    }
}