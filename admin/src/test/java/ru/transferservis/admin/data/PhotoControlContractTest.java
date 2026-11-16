package ru.transferservis.admin.data;
import com.google.gson.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class PhotoControlContractTest {
    static JsonObject object(String s){return JsonParser.parseString(s).getAsJsonObject();}
    static JsonObject vehicle(){return object("{\"id\":\"vehicle_fixture001\",\"driverId\":\"staff_driver001\",\"make\":\"Haval\",\"model\":\"F7\",\"plate\":\"TEST-01\",\"status\":\"approved\",\"revision\":7,\"lastPhotoCheckPassedAt\":null,\"nextPhotoCheckDueAt\":null}");}
    static JsonObject vehicles(){JsonObject b=object("{\"ok\":true,\"vehicles\":[],\"nextCursor\":null,\"requestId\":\"req_fixture001\"}");b.getAsJsonArray("vehicles").add(vehicle());return b;}
    static PhotoControl.Vehicle parsed()throws IOException{return PhotoControl.Vehicles.parse(vehicles(),"").rows.get(0);}
    static JsonObject checks(){return object("{\"ok\":true,\"vehicleId\":\"vehicle_fixture001\",\"vehicleRevision\":7,\"checks\":[],\"nextCursor\":null,\"requestId\":\"req_fixture001\"}");}
    static PhotoControl.Checks page()throws IOException{return PhotoControl.Checks.parse(checks(),parsed(),"");}
    static AdminAccess access(){return AssignmentTestData.access("vehicles.read","vehicles.verify");}
    private interface Invalid {void run()throws Exception;}
    private void rejects(Invalid action)throws Exception{try{action.run();fail("Invalid data accepted");}catch(IOException expected){}}
    @Test public void exactProjectionRejectsPrivateAndUnknownFields()throws Exception {
        assertEquals("Haval",parsed().make);for(String key:new String[]{"vin","phone","reason","photos","privateObjectKey"}){JsonObject b=vehicles();b.getAsJsonArray("vehicles").get(0).getAsJsonObject().addProperty(key,"PRIVATE");rejects(()->PhotoControl.Vehicles.parse(b,""));}
    }
    @Test public void typeRevisionAndIdentifierValidationFailClosed()throws Exception{
        for(String value:new String[]{"0","-1","1.5","9007199254740992","\"7\"","true"}){JsonObject b=vehicles();b.getAsJsonArray("vehicles").get(0).getAsJsonObject().add("revision",JsonParser.parseString(value));rejects(()->PhotoControl.Vehicles.parse(b,""));}
        JsonObject b=vehicles();b.getAsJsonArray("vehicles").get(0).getAsJsonObject().addProperty("id","../PRIVATE");rejects(()->PhotoControl.Vehicles.parse(b,""));
    }
    @Test public void duplicatesOversizeAndNonadvancingCursorAreRejected()throws Exception{
        JsonObject oversized=vehicles();for(int i=0;i<50;i++)oversized.getAsJsonArray("vehicles").add(vehicle());rejects(()->PhotoControl.Vehicles.parse(oversized,""));
        JsonObject duplicate=vehicles();duplicate.getAsJsonArray("vehicles").add(vehicle());rejects(()->PhotoControl.Vehicles.parse(duplicate,""));
        JsonObject repeated=vehicles();repeated.addProperty("nextCursor","cursor_fixture");rejects(()->PhotoControl.Vehicles.parse(repeated,"cursor_fixture"));
        JsonObject empty=vehicles();empty.getAsJsonArray("vehicles").remove(0);empty.addProperty("nextCursor","cursor_fixture");rejects(()->PhotoControl.Vehicles.parse(empty,""));
    }
    @Test public void checkPageBindsVehicleAndVehicleRevision()throws Exception{
        JsonObject wrong=checks();wrong.addProperty("vehicleId","vehicle_other001");rejects(()->PhotoControl.Checks.parse(wrong,parsed(),""));
        JsonObject b=checks();b.addProperty("vehicleRevision",8);try{PhotoControl.Checks.parse(b,parsed(),"");fail();}catch(AdminApiClient.Failure e){assertEquals(409,e.status);}
    }
    @Test public void everyCheckBindsVehicleAndRejectsPhotoMaterial()throws Exception{
        JsonObject c=object("{\"id\":\"photo-check_fixture001\",\"vehicleId\":\"vehicle_fixture001\",\"kind\":\"manual\",\"restrictionMode\":\"immediate\",\"result\":\"pending\",\"revision\":1,\"dueAt\":1,\"graceUntil\":1,\"createdAt\":1,\"updatedAt\":1}");
        JsonObject body=checks();body.getAsJsonArray("checks").add(c);assertEquals(1,PhotoControl.Checks.parse(body,parsed(),"").rows.size());
        c.addProperty("photos","PRIVATE");rejects(()->PhotoControl.Checks.parse(body,parsed(),""));c.remove("photos");c.addProperty("vehicleId","vehicle_other001");rejects(()->PhotoControl.Checks.parse(body,parsed(),""));
    }
    @Test public void actionRequiresExplicitUnplannedKindModeAndNonblankReason()throws Exception{
        PhotoControl.Checks p=page();for(String kind:new String[]{"","monthly","other"})rejects(()->PhotoCheckAction.create(access(),1,p,kind,"immediate","Reason"));
        for(String mode:new String[]{"","grace_period","other"})rejects(()->PhotoCheckAction.create(access(),1,p,"manual",mode,"Reason"));
        for(String reason:new String[]{""," \u00a0\uFEFF ","x".repeat(501),"private\u0000"})rejects(()->PhotoCheckAction.create(access(),1,p,"manual","immediate",reason));
        rejects(()->PhotoCheckAction.create(AssignmentTestData.access("vehicles.read"),1,p,"manual","immediate","Reason"));
    }
    @Test public void actionBodyUsesVehicleRevisionAndPreservesExactIntent()throws Exception{
        PhotoCheckAction a=PhotoCheckAction.create(access(),4,page(),"complaint","after_current_trip"," \u00a0Reason\nTwo \uFEFF");assertEquals(4,a.body().size());assertEquals(7,a.body().get("expectedRevision").getAsLong());assertEquals("Reason\nTwo",a.reason);
    }
    @Test public void acknowledgementMustMatchVehicleKindModeAndIncrementedVehicleRevision()throws Exception{
        PhotoCheckAction a=PhotoCheckAction.create(access(),4,page(),"manual","immediate","Reason");JsonObject b=object("{\"ok\":true,\"photoCheck\":{\"id\":\"photo-check_fixture001\",\"vehicleId\":\"vehicle_fixture001\",\"kind\":\"manual\",\"restrictionMode\":\"immediate\",\"result\":\"pending\",\"revision\":1,\"vehicleRevision\":8}}");a.validateAck(b);
        for(String key:new String[]{"vehicleId","kind","restrictionMode","result","id"}){JsonObject bad=b.deepCopy();bad.getAsJsonObject("photoCheck").addProperty(key,"wrong");rejects(()->a.validateAck(bad));}
        b.getAsJsonObject("photoCheck").addProperty("vehicleRevision",7);rejects(()->a.validateAck(b));
    }
    @Test public void transportUsesScopedSafeQueriesAndNeverAutoReplaysPost()throws Exception{
        AtomicInteger calls=new AtomicInteger();PhotoCheckAction a=PhotoCheckAction.create(access(),4,page(),"manual","immediate","Reason");
        AdminApiClient api=new AdminApiClient(chain->{calls.incrementAndGet();assertEquals("Bearer synthetic",chain.request().header("Authorization"));assertEquals("no-store",chain.request().header("Cache-Control"));assertEquals(Long.valueOf(4),chain.request().tag(Long.class));
            if("POST".equals(chain.request().method())){assertTrue(chain.request().body().isOneShot());assertEquals(a.key,chain.request().header("Idempotency-Key"));okio.Buffer bytes=new okio.Buffer();chain.request().body().writeTo(bytes);assertEquals(a.body(),JsonParser.parseString(bytes.readUtf8()));}
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(503).message("Synthetic").header("Retry-After","0").body(ResponseBody.create(MediaType.get("application/json"),"{\"ok\":false,\"error\":\"PRIVATE_PROVIDER\"}")).build();});
        rejects(()->api.photoVehicles("synthetic","approved","cursor&PRIVATE",4));assertEquals(0,calls.get());
        for(int n=1;n<=2;n++){try{api.photoCommand("synthetic",a);fail();}catch(AdminApiClient.Failure e){assertEquals(503,e.status);assertFalse(e.getMessage().contains("PRIVATE"));}assertEquals(n,calls.get());}
    }
}
