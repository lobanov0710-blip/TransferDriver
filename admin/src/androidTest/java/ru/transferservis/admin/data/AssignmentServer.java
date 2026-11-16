package ru.transferservis.admin.data;

import com.google.gson.*;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;

/** Synthetic wire contract only. Production origin/epoch checks run before this interceptor. */
final class AssignmentServer implements Interceptor {
    static final String ACTOR="staff_operator001",ORDER="order_native001",CAR="vehicle_native001",CAR2="vehicle_native002",DRIVER="staff_native001";
    final AtomicInteger orders=new AtomicInteger(),commands=new AtomicInteger(),commits=new AtomicInteger(),me=new AtomicInteger();
    boolean phoneScope,owner,mfa=true,canAssign=true,secondBlocked=true,paginate,lostAck;
    int failure;long revision=7;String state="new",assignedVehicle=CAR,lastKey="",lastBody="",lastPath="";
    JsonObject cachedAck;CountDownLatch entered,release;String holdPath="";
    static AdminSession session(String suffix){long until=System.currentTimeMillis()+3600000;return new AdminSession(ACTOR,"Synthetic Operator","synthetic-"+suffix,until,"refresh-"+suffix,until);}
    private Response reply(Chain chain,int status,JsonObject body){return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(status).message("Synthetic").body(ResponseBody.create(MediaType.get("application/json"),body.toString())).build();}
    static JsonObject object(String json){return JsonParser.parseString(json).getAsJsonObject();}
    JsonObject profile(){
        JsonObject body=object("{\"ok\":true,\"admin\":{\"accountId\":\""+ACTOR+"\",\"role\":\"dispatcher\",\"permissions\":[\"orders.read\",\"drivers.read\",\"vehicles.read\",\"orders.reassign\"]}}");JsonObject admin=body.getAsJsonObject("admin");
        if(canAssign)admin.getAsJsonArray("permissions").add("orders.assign");
        if(owner||phoneScope){admin.addProperty("role","owner");JsonArray all=new JsonArray();all.add("*");admin.add("permissions",all);JsonObject proof=object("{\"required\":true,\"verified\":true,\"method\":\"totp\"}");proof.addProperty("verified",mfa);proof.addProperty("expiresAt",System.currentTimeMillis()+3600000);admin.add("mfa",proof);}
        if(phoneScope)admin.addProperty("testScope","owner_phone");return body;
    }
    JsonObject order(String id){JsonObject row=object("{\"id\":\""+id+"\",\"from\":\"Отправление\",\"to\":\"Прибытие\",\"date\":\"2030-01-01\",\"tariff\":\"comfort\",\"status\":\"new\",\"revision\":7,\"assignment\":null}");
        if(ORDER.equals(id)){row.addProperty("revision",revision);row.addProperty("status",state);if(!"new".equals(state))row.add("assignment",object("{\"id\":\"assignment_native001\",\"driverId\":\""+DRIVER+"\",\"vehicleId\":\""+assignedVehicle+"\",\"revision\":1,\"phase\":\"assigned\"}"));}return row;}
    JsonObject options(){JsonObject body=object("{\"ok\":true,\"orderId\":\""+ORDER+"\",\"orderRevision\":7,\"options\":[],\"nextCursor\":null}");body.addProperty("orderRevision",revision);
        for(String car:new String[]{CAR,CAR2}){JsonObject option=object("{\"driverId\":\""+DRIVER+"\",\"fullName\":\"Синтетический водитель\",\"vehicleId\":\""+car+"\",\"make\":\"Synthetic\",\"model\":\"Car\",\"plate\":\"SYNTH-01\",\"status\":\"approved\",\"availability\":{\"state\":\"off_line\",\"activeVehicleId\":null},\"eligible\":true,\"blockers\":[]}");
            if(CAR2.equals(car))option.addProperty("plate","SYNTH-02");String blocker=(!"new".equals(state)&&assignedVehicle.equals(car))?"assignment_unchanged":CAR2.equals(car)&&secondBlocked?"vehicle_photo_control_required":"";
            if(!blocker.isEmpty()){option.addProperty("eligible",false);option.getAsJsonArray("blockers").add(blocker);}body.getAsJsonArray("options").add(option);}return body;
    }
    @Override public Response intercept(Chain chain)throws IOException {
        String path=chain.request().url().encodedPath();org.junit.Assert.assertEquals("Bearer synthetic-original",chain.request().header("Authorization"));
        if(path.equals(holdPath)&&entered!=null){entered.countDown();try{org.junit.Assert.assertTrue(release.await(8,TimeUnit.SECONDS));}catch(InterruptedException stop){Thread.currentThread().interrupt();throw new IOException("Synthetic cancelled");}}
        if(path.equals("/v1/admin/me")){me.incrementAndGet();return reply(chain,200,profile());}
        if(path.equals("/v1/admin/orders")){orders.incrementAndGet();org.junit.Assert.assertEquals("20",chain.request().url().queryParameter("limit"));
            JsonObject body=object("{\"ok\":true,\"orders\":[],\"nextCursor\":null}");String status=chain.request().url().queryParameter("status");
            if(status==null||status.equals(state))body.getAsJsonArray("orders").add(order(chain.request().url().queryParameter("cursor")==null?ORDER:"order_native002"));
            if(paginate&&chain.request().url().queryParameter("cursor")==null)body.addProperty("nextCursor","page_native2");return reply(chain,200,body);}
        if(path.endsWith("/assignment-options"))return reply(chain,200,options());
        org.junit.Assert.assertTrue(path.endsWith("/assign")||path.endsWith("/reassign"));org.junit.Assert.assertTrue(chain.request().body().isOneShot());commands.incrementAndGet();lastPath=path;
        okio.Buffer bytes=new okio.Buffer();chain.request().body().writeTo(bytes);String raw=bytes.readUtf8(),key=chain.request().header("Idempotency-Key");JsonObject body=object(raw);org.junit.Assert.assertEquals(4,body.size());
        org.junit.Assert.assertTrue(key.startsWith("assignment-"));
        if(failure!=0)return reply(chain,failure,object("{\"ok\":false,\"code\":\"synthetic_failure\",\"error\":\"PRIVATE_SYNTHETIC_PROVIDER\"}")).newBuilder().header("Retry-After","0").build();
        if(key.equals(lastKey)){org.junit.Assert.assertEquals(lastBody,raw);return reply(chain,200,cachedAck);}
        org.junit.Assert.assertEquals(revision,body.get("expectedRevision").getAsLong());lastKey=key;lastBody=raw;commits.incrementAndGet();state="taken";revision++;assignedVehicle=body.get("vehicleId").getAsString();
        cachedAck=object("{\"ok\":true,\"assignment\":{\"id\":\"assignment_native001\",\"state\":\"active\",\"revision\":1,\"orderId\":\""+ORDER+"\",\"driverId\":\""+DRIVER+"\",\"vehicleId\":\""+assignedVehicle+"\",\"orderRevision\":"+revision+"}}");
        if(lostAck){lostAck=false;throw new IOException("Synthetic lost acknowledgement");}return reply(chain,200,cachedAck);
    }
}
