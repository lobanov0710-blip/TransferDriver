package ru.transferservis.admin.security;
import ru.transferservis.admin.BuildConfig;
import java.net.URI;
import okhttp3.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class AdminEndpointPolicyTest {
    @Test public void allVariantsUseLockedEndpointAndDebugFlag() {
        boolean qa="qa".equals(BuildConfig.BUILD_TYPE);
        boolean staging="debug".equals(BuildConfig.BUILD_TYPE)||"staging".equals(BuildConfig.BUILD_TYPE)||qa;
        assertTrue(staging||"release".equals(BuildConfig.BUILD_TYPE));
        assertEquals(staging?AdminEndpointPolicy.STAGING:AdminEndpointPolicy.PRODUCTION,AdminEndpointPolicy.BASE_URL);
        assertEquals("debug".equals(BuildConfig.BUILD_TYPE),BuildConfig.DEBUG);
        assertEquals(qa?"ru.transferservis.admin.qa":"ru.transferservis.admin",BuildConfig.APPLICATION_ID);
        assertTrue(!qa || BuildConfig.VERSION_CODE >= 2);
        assertTrue(AdminEndpointPolicy.allows(URI.create(AdminEndpointPolicy.BASE_URL+"v1/admin/me")));
        assertFalse(AdminEndpointPolicy.allows(URI.create(staging?AdminEndpointPolicy.PRODUCTION:AdminEndpointPolicy.STAGING)));
    }
    @Test public void exfiltrationTargetsAndTransportRetriesAreRejected() throws Exception {
        for(String url:new String[]{"http://uber-v3-staging.lobanov0710.workers.dev/","https://evil.example/",
                AdminEndpointPolicy.BASE_URL.replace(".dev/",".dev.evil.example/"),AdminEndpointPolicy.BASE_URL.replace("https://","https://x@"),
                AdminEndpointPolicy.BASE_URL.replace(".dev/",".dev:444/"),AdminEndpointPolicy.BASE_URL+"#secret"})
            assertFalse(url,AdminEndpointPolicy.allows(URI.create(url)));
        OkHttpClient client=AdminEndpointPolicy.clientBuilder().addInterceptor(chain->{fail("Credentials must not be sent");return null;}).build();
        assertFalse(client.followRedirects());assertFalse(client.followSslRedirects());assertFalse(client.retryOnConnectionFailure());
        try {client.newCall(new Request.Builder().url("https://evil.example/").header("Authorization","Bearer fixture-secret").build()).execute();fail();}
        catch(java.io.IOException expected){assertFalse(expected.getMessage().contains("fixture-secret"));}
    }

    @Test public void mutationBodiesAreOneShotBeforeTransport() throws Exception {
        okhttp3.OkHttpClient client=AdminEndpointPolicy.clientBuilder().addInterceptor(chain->{
            assertTrue(chain.request().body().isOneShot());
            return new okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(503).message("Synthetic").header("Retry-After","0").body(okhttp3.ResponseBody.create(null,"")).build();
        }).build();
        for(String method:new String[]{"POST","PUT","PATCH"})try(okhttp3.Response response=client.newCall(new okhttp3.Request.Builder()
                .url(AdminEndpointPolicy.BASE_URL+"synthetic-mutation").method(method,okhttp3.RequestBody.create(null,"{}")).build()).execute()) {
            assertEquals(503,response.code());
        }
    }
}
