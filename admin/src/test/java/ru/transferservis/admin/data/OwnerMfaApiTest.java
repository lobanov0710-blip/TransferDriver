package ru.transferservis.admin.data;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
public final class OwnerMfaApiTest {
    @Test public void totpRecoveryAndEnrollmentUseBodyOnlyWithoutPretendAuthorization() throws Exception {
        String[] paths={"/v1/admin/mfa/totp/verify","/v1/admin/mfa/recovery/verify","/v1/admin/mfa/enrollment/start","/v1/admin/mfa/enrollment/confirm"};
        AtomicInteger calls=new AtomicInteger();
        AdminApiClient api=new AdminApiClient(chain->{
            Request request=chain.request();int i=calls.getAndIncrement();assertEquals(paths[i],request.url().encodedPath());assertNull(request.url().query());
            assertEquals("POST",request.method());assertNull(request.header("Authorization"));assertNull(request.header("CF-Connecting-IP"));
            okio.Buffer buffer=new okio.Buffer();request.body().writeTo(buffer);JsonObject body=JsonParser.parseString(buffer.readUtf8()).getAsJsonObject();assertEquals(2,body.size());
            if(i==0)assertEquals("123456",body.get("code").getAsString());if(i==1)assertEquals("fixture-recovery",body.get("recoveryCode").getAsString());
            if(i==2)assertEquals("fixture-bootstrap",body.get("bootstrapCode").getAsString());if(i==3)assertEquals("fixture-enrollment",body.get("enrollmentId").getAsString());
            return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("fixture").body(ResponseBody.create(MediaType.get("application/json"),"{\"ok\":true}")).build();
        });
        api.verifyMfa("fixture-challenge","123456",false);api.verifyMfa("fixture-challenge","fixture-recovery",true);
        api.startEnrollment("fixture-challenge","fixture-bootstrap");api.confirmEnrollment("fixture-enrollment","123456");assertEquals(4,calls.get());
    }
}
