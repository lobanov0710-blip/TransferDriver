package ru.transferservis.admin.security;

import java.net.URI;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import ru.transferservis.admin.BuildConfig;

public final class AdminEndpointPolicy {
    public static final String STAGING = "https://uber-v3-staging.lobanov0710.workers.dev/";
    public static final String PRODUCTION = "https://uber-v3.lobanov0710.workers.dev/";
    public static final String BASE_URL = BuildConfig.ADMIN_API_BASE_URL;
    private static final URI BASE = URI.create(BASE_URL);
    private AdminEndpointPolicy() {}
    public static boolean allows(URI uri) {
        return (STAGING.equals(BASE_URL) || PRODUCTION.equals(BASE_URL)) && uri != null
                && "https".equalsIgnoreCase(uri.getScheme()) && BASE.getHost().equalsIgnoreCase(uri.getHost())
                && (uri.getPort() == -1 || uri.getPort() == 443)
                && uri.getRawUserInfo() == null && uri.getRawFragment() == null;
    }
    public static OkHttpClient.Builder clientBuilder() {
        return new OkHttpClient.Builder().connectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS))
                .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
                .callTimeout(30, TimeUnit.SECONDS).addInterceptor(chain -> {
                    if (!allows(chain.request().url().uri())) throw new IOException("Untrusted endpoint");
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
