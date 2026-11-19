package ru.transferservis.admin.data;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class AdminApiClientTest {
    private Response response(Request request,int status,String body) {
        return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(status).message("fixture")
                .body(ResponseBody.create(MediaType.get("application/json"),body)).build();
    }
    @Test public void loginAndRefreshUseExistingContractWithoutAuthorizationHeader() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        AdminApiClient api=new AdminApiClient(chain->{
            Request request=chain.request();assertEquals("POST",request.method());assertNull(request.header("Authorization"));
            okio.Buffer buffer=new okio.Buffer();request.body().writeTo(buffer);
            JsonObject body=JsonParser.parseString(buffer.readUtf8()).getAsJsonObject();
            if(calls.getAndIncrement()==0){assertEquals("/staff/login",request.url().encodedPath());assertEquals("admin.fixture",body.get("login").getAsString());assertEquals("fixture-password",body.get("password").getAsString());}
            else {assertEquals("/staff/refresh",request.url().encodedPath());assertEquals("fixture-refresh",body.get("refreshToken").getAsString());}
            return response(request,200,"{\"ok\":true}");
        });
        api.login("admin.fixture","fixture-password");api.refresh("fixture-refresh");assertEquals(2,calls.get());
    }
    @Test public void meAndQueueSendBearerOnlyToApprovedBackend() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        AdminApiClient api=new AdminApiClient(chain->{
            assertEquals("Bearer fixture-access",chain.request().header("Authorization"));
            assertEquals("GET",chain.request().method());
            if(calls.getAndIncrement()==0)assertEquals("/v1/admin/me",chain.request().url().encodedPath());
            else {assertEquals("/v1/admin/registrations",chain.request().url().encodedPath());assertEquals("50",chain.request().url().queryParameter("limit"));}
            return response(chain.request(),200,"{\"ok\":true}");
        });
        api.me("fixture-access");api.registrations("fixture-access");assertEquals(2,calls.get());
    }
    @Test public void redirectAndErrorsDoNotExposeBodiesOrReplayLogin() throws Exception {
        for(int status:new int[]{307,401,403,429,500}) {
            AtomicInteger calls=new AtomicInteger();
            AdminApiClient api=new AdminApiClient(chain->{calls.incrementAndGet();return response(chain.request(),status,"upstream-private-fixture");});
            try {api.login("fixture","fixture-password");fail();}
            catch(AdminApiClient.Failure expected){assertEquals(status,expected.status);assertFalse(expected.getMessage().contains("upstream-private-fixture"));}
            assertEquals(1,calls.get());
        }
    }
    @Test public void malformedAndOversizedResponsesAreRejected() throws Exception {
        for(String body:new String[]{"not-json",new String(new char[1_048_577]).replace('\0','x')}) {
            AdminApiClient api=new AdminApiClient(chain->response(chain.request(),200,body));
            try {api.me("fixture-access");fail();}catch(java.io.IOException expected){assertTrue(expected.getMessage().length()<80);}
        }
    }
}
