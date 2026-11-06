package ru.transferservis.driver.security;

import java.net.URI;

/** Only the authoritative backend may receive credentials or passenger data. */
public final class ApiEndpointPolicy {

    public static final String BASE_URL =
            "https://uber-v3.lobanov0710.workers.dev/";

    private static final String HOST = URI.create(BASE_URL).getHost();

    private ApiEndpointPolicy() {
    }

    public static boolean isAllowed(URI endpoint) {
        return endpoint != null
                && "https".equalsIgnoreCase(endpoint.getScheme())
                && HOST.equalsIgnoreCase(endpoint.getHost())
                && (endpoint.getPort() == -1 || endpoint.getPort() == 443)
                && endpoint.getRawUserInfo() == null
                && endpoint.getRawFragment() == null;
    }
}
