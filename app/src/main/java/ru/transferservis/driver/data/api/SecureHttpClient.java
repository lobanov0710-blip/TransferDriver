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
                    okhttp3.Request request = chain.request();
                    if (request.body() != null) request = request.newBuilder()
                            .method(request.method(), oneShot(request.body())).build();
                    return chain.proceed(request);
                });
    }

    /** Also blocks OkHttp's HTTP 503 Retry-After: 0 follow-up, independent of connection retries. */
    public static okhttp3.RequestBody oneShot(okhttp3.RequestBody body) {
        if (body.isOneShot()) return body;
        return new okhttp3.RequestBody() {
            @Override public okhttp3.MediaType contentType() { return body.contentType(); }
            @Override public long contentLength() throws IOException { return body.contentLength(); }
            @Override public boolean isDuplex() { return body.isDuplex(); }
            @Override public boolean isOneShot() { return true; }
            @Override public void writeTo(okio.BufferedSink sink) throws IOException { body.writeTo(sink); }
        };
    }
}
