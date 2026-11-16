package ru.transferservis.admin.data;
import com.google.gson.*;
import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;
public final class AssignmentTransportTest {
    private Response response(Interceptor.Chain c,int status,JsonObject body){return new Response.Builder().request(c.request()).protocol(Protocol.HTTP_1_1).code(status).message("Synthetic").body(ResponseBody.create(MediaType.get("application/json"),body.toString())).build();}
    @Test public void queriesAndScopeAreBoundAndUnsafeCursorNeverReachesTransport()throws Exception {
        AtomicInteger calls=new AtomicInteger();AdminApiClient api=new AdminApiClient(c->{assertEquals(Long.valueOf(3),c.request().tag(Long.class));assertEquals("Bearer synthetic",c.request().header("Authorization"));assertEquals("no-store",c.request().header("Cache-Control"));calls.incrementAndGet();return response(c,200,AssignmentTestData.orders());});
        api.assignmentOrders("synthetic","new","cursor_value",3);assertEquals(1,calls.get());
        try{api.assignmentOrders("synthetic","new","cursor&actor=PRIVATE",3);fail();}catch(IOException expected){}assertEquals(1,calls.get());
    }
    @Test public void explicitMutationReusesBodyKeyAnd503HasNoAutomaticReplayOrRawError()throws Exception {
        Assignments.Options page=AssignmentTestData.parsedOptions();AssignmentAction action=AssignmentAction.create(AssignmentTestData.all(),4,page,page.rows.get(0),"Reason");AtomicInteger calls=new AtomicInteger();
        AdminApiClient api=new AdminApiClient(c->{assertEquals("POST",c.request().method());assertTrue(c.request().url().encodedPath().endsWith("/assign"));assertEquals(action.key,c.request().header("Idempotency-Key"));assertEquals(Long.valueOf(4),c.request().tag(Long.class));assertTrue(c.request().body().isOneShot());
            okio.Buffer b=new okio.Buffer();c.request().body().writeTo(b);assertEquals(action.body(),JsonParser.parseString(b.readUtf8()));calls.incrementAndGet();
            return response(c,503,JsonParser.parseString("{\"ok\":false,\"code\":\"service_unavailable\",\"error\":\"PRIVATE_PROVIDER\"}").getAsJsonObject()).newBuilder().header("Retry-After","0").build();});
        for(int i=0;i<2;i++){try{api.assignmentCommand("synthetic",action);fail();}catch(AdminApiClient.Failure expected){assertEquals(503,expected.status);assertFalse(expected.getMessage().contains("PRIVATE_PROVIDER"));}assertEquals(i+1,calls.get());}
    }
    @Test public void acknowledgementMustMatchOriginalOrderVehicleAndRevision()throws Exception {
        Assignments.Options page=AssignmentTestData.parsedOptions();AssignmentAction action=AssignmentAction.create(AssignmentTestData.all(),4,page,page.rows.get(0),"Reason");
        JsonObject body=JsonParser.parseString("{\"ok\":true,\"assignment\":{\"id\":\"assignment_fixture001\",\"state\":\"active\",\"revision\":1,\"orderId\":\"order_fixture001\",\"driverId\":\"staff_driver001\",\"vehicleId\":\"vehicle_fixture001\",\"orderRevision\":8}}").getAsJsonObject();action.validateAck(body);
        for(String key:new String[]{"orderId","driverId","vehicleId","id"}){JsonObject bad=body.deepCopy();bad.getAsJsonObject("assignment").addProperty(key,"wrong_target");try{action.validateAck(bad);fail();}catch(IOException expected){}}
        JsonObject bad=body.deepCopy();bad.getAsJsonObject("assignment").addProperty("orderRevision",7);try{action.validateAck(bad);fail();}catch(IOException expected){}
    }

}
