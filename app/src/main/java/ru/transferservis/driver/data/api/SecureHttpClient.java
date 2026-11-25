package ru.transferservis.driver.data.api;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;

import ru.transferservis.driver.security.ApiEndpointPolicy;

public final class SecureHttpClient {

    private SecureHttpClient() {
    }

    public static OkHttpClient.Builder newBuilder() {
        return new OkHttpClient.Builder()
                .connectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS))
                .followRedirects(false)
                .followSslRedirects(false)
                // A rotated refresh token or order mutation must not be replayed.
                .retryOnConnectionFailure(false)
                .callTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    if (!ApiEndpointPolicy.isAllowed(chain.request().url().uri())) {
                        // Do not include the URL, request body or credentials.
                        throw new IOException("Untrusted API endpoint");
                    }
                    return chain.proceed(chain.request());
                });
    }
}
