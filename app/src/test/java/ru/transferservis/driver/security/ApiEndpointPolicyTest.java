package ru.transferservis.driver.security;

import java.net.URI;

import org.junit.Test;

import ru.transferservis.driver.BuildConfig;

import static org.junit.Assert.*;

public class ApiEndpointPolicyTest {

    private static boolean usesStagingBackend() {
        switch (BuildConfig.BUILD_TYPE) {
            case "debug":
            case "staging":
                return true;
            case "release":
                return false;
            default:
                throw new AssertionError("Unverified build type: " + BuildConfig.BUILD_TYPE);
        }
    }

    @Test
    public void buildVariantUsesLockedEnvironmentEndpoint() {

        assertEquals("debug".equals(BuildConfig.BUILD_TYPE), BuildConfig.DEBUG);

        assertEquals(
                usesStagingBackend()
                        ? ApiEndpointPolicy.STAGING_BASE_URL
                        : ApiEndpointPolicy.PRODUCTION_BASE_URL,
                ApiEndpointPolicy.BASE_URL
        );

        assertTrue(
                ApiEndpointPolicy.isAllowed(
                        URI.create(
                                ApiEndpointPolicy.BASE_URL
                        )
                )
        );

        String configuredHostUrl =
                usesStagingBackend()
                        ? "https://UBER-V3-STAGING.LOBANOV0710.WORKERS.DEV:443/v1/driver/me"
                        : "https://UBER-V3.LOBANOV0710.WORKERS.DEV:443/v1/driver/me";

        assertTrue(
                ApiEndpointPolicy.isAllowed(
                        URI.create(
                                configuredHostUrl
                        )
                )
        );
    }


    @Test
    public void configuredHostDoesNotAllowCrossEnvironmentCredentials() {

        assertFalse(
                ApiEndpointPolicy.isAllowed(
                        URI.create(
                                usesStagingBackend()
                                        ? ApiEndpointPolicy.PRODUCTION_BASE_URL
                                        : ApiEndpointPolicy.STAGING_BASE_URL
                        )
                )
        );
    }


    @Test
    public void rejectsCredentialExfiltrationTargets() {

        String[] attacks = {
                "http://uber-v3-staging.lobanov0710.workers.dev/staff/login",
                "https://uber-v3-staging.lobanov0710.workers.dev.attacker.example/",
                "https://uber-v3-staging.lobanov0710.workers.dev@attacker.example/",
                "https://attacker.example@uber-v3-staging.lobanov0710.workers.dev/",
                "https://uber-v3-staging.lobanov0710.workers.dev:444/",
                "https://uber-v3-staging.lobanov0710.workers.dev./",
                "https://uber-v3-staging.lobanov0710.workers.dev/#secret",
                "//uber-v3-staging.lobanov0710.workers.dev/",
                "file:///staff/refresh",
                "https://127.0.0.1/"
        };

        for (String attack : attacks) {

            assertFalse(
                    attack,
                    ApiEndpointPolicy.isAllowed(
                            URI.create(
                                    attack
                            )
                    )
            );
        }

        assertFalse(
                ApiEndpointPolicy.isAllowed(
                        null
                )
        );
    }
}
