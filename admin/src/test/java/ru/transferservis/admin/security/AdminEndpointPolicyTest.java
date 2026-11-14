package ru.transferservis.admin.security;
import ru.transferservis.admin.BuildConfig;
import java.net.URI;
import okhttp3.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class AdminEndpointPolicyTest {
    @Test public void allVariantsUseLockedEndpointAndDebugFlag() {
        boolean staging="debug".equals(BuildConfig.BUILD_TYPE)||"staging".equals(BuildConfig.BUILD_TYPE);
        assertTrue(staging||"release".equals(BuildConfig.BUILD_TYPE));
        assertEquals(staging?AdminEndpointPolicy.STAGING:AdminEndpointPolicy.PRODUCTION,AdminEndpointPolicy.BASE_URL);
        assertEquals("debug".equals(BuildConfig.BUILD_TYPE),BuildConfig.DEBUG);
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
}
