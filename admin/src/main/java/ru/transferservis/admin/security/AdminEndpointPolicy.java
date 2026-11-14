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
                    return chain.proceed(chain.request());
                });
    }
}
