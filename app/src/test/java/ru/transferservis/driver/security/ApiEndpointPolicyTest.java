package ru.transferservis.driver.security;

import java.net.URI;
import org.junit.Test;
import static org.junit.Assert.*;

public class ApiEndpointPolicyTest {
    @Test public void acceptsBackendHttpsAndDefaultPort() {
        assertTrue(ApiEndpointPolicy.isAllowed(URI.create(ApiEndpointPolicy.BASE_URL)));
        assertTrue(ApiEndpointPolicy.isAllowed(URI.create("https://UBER-V3.LOBANOV0710.WORKERS.DEV:443/staff/refresh")));
    }

    @Test public void rejectsCredentialExfiltrationTargets() {
        String[] attacks = {
            "http://uber-v3.lobanov0710.workers.dev/staff/login",
            "https://uber-v3.lobanov0710.workers.dev.attacker.example/",
            "https://uber-v3.lobanov0710.workers.dev@attacker.example/",
            "https://attacker.example@uber-v3.lobanov0710.workers.dev/",
            "https://uber-v3.lobanov0710.workers.dev:444/",
            "https://uber-v3.lobanov0710.workers.dev./",
            "https://uber-v3.lobanov0710.workers.dev/#secret",
            "//uber-v3.lobanov0710.workers.dev/",
            "file:///staff/refresh", "https://127.0.0.1/"
        };
        for (String attack : attacks) {
            assertFalse(attack, ApiEndpointPolicy.isAllowed(URI.create(attack)));
        }
        assertFalse(ApiEndpointPolicy.isAllowed(null));
    }
}
