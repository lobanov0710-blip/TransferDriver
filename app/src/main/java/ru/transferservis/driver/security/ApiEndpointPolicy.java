package ru.transferservis.driver.security;

import java.net.URI;

import ru.transferservis.driver.BuildConfig;

/** Only the authoritative backend may receive credentials or passenger data. */
public final class ApiEndpointPolicy {

    public static final String PRODUCTION_BASE_URL =
            "https://uber-v3.lobanov0710.workers.dev/";

    public static final String STAGING_BASE_URL =
            "https://uber-v3-staging.lobanov0710.workers.dev/";

    public static final String BASE_URL =
            BuildConfig.DRIVER_API_BASE_URL;

    private static final URI CONFIGURED_URI =
            URI.create(
                    BASE_URL
            );

    private static final String HOST =
            CONFIGURED_URI.getHost();

    private ApiEndpointPolicy() {
    }

    public static boolean isAllowed(URI endpoint) {
        return endpoint != null
                && isSupportedConfiguredEndpoint()
                && "https".equalsIgnoreCase(endpoint.getScheme())
                && HOST.equalsIgnoreCase(endpoint.getHost())
                && (endpoint.getPort() == -1 || endpoint.getPort() == 443)
                && endpoint.getRawUserInfo() == null
                && endpoint.getRawFragment() == null;
    }

    private static boolean isSupportedConfiguredEndpoint() {

        String configured =
                CONFIGURED_URI
                        .toString();

        return PRODUCTION_BASE_URL.equals(
                configured
        )
                ||
                STAGING_BASE_URL.equals(
                        configured
                );
    }
}
