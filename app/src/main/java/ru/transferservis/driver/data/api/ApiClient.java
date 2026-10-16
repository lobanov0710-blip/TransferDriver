package ru.transferservis.driver.data.api;

import android.content.Context;

import okhttp3.OkHttpClient;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import ru.transferservis.driver.data.session.SecureSessionStore;

public final class ApiClient {

    static final String BASE_URL =
            "https://uber-v3.lobanov0710.workers.dev/";

    private static Retrofit retrofit;

    private ApiClient() {
    }

    public static synchronized void initialize(
            Context context
    ) {

        if (retrofit != null) {
            return;
        }

        Context applicationContext =
                context
                        .getApplicationContext();

        SecureSessionStore sessionStore =
                new SecureSessionStore(
                        applicationContext
                );

        StaffAuthInterceptor authInterceptor =
                new StaffAuthInterceptor(
                        sessionStore
                );

        OkHttpClient okHttpClient =
                new OkHttpClient.Builder()
                        .addInterceptor(
                                authInterceptor
                        )
                        .build();

        retrofit =
                new Retrofit.Builder()
                        .baseUrl(
                                BASE_URL
                        )
                        .client(
                                okHttpClient
                        )
                        .addConverterFactory(
                                GsonConverterFactory.create()
                        )
                        .build();
    }

    public static Retrofit getRetrofit() {

        if (retrofit == null) {

            throw new IllegalStateException(
                    "ApiClient is not initialized"
            );
        }

        return retrofit;
    }

    public static StaffApi getStaffApi() {

        return getRetrofit()
                .create(
                        StaffApi.class
                );
    }

    public static OrderApi getOrderApi() {

        return getRetrofit()
                .create(
                        OrderApi.class
                );
    }
}