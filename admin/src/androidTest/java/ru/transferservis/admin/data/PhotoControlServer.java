package ru.transferservis.admin.data;
import com.google.gson.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import static org.junit.Assert.*;
final class PhotoControlServer implements Interceptor {
    static final String ACTOR="staff_operator001",CAR="vehicle_native001",CAR2="vehicle_native002",DRIVER="staff_native001";
    final AtomicInteger vehicles=new AtomicInteger(),commands=new AtomicInteger(),commits=new AtomicInteger();
    boolean read=true,verify=true,owner,mfa=true,phoneScope,lostAck,paginate;int failure;long revision=7;
    String lastKey="",lastBody="",holdPath="";JsonObject cachedAck;CountDownLatch entered,release;
    static JsonObject object(String s){return JsonParser.parseString(s).getAsJsonObject();}
    static AdminSession session(String suffix){return AssignmentServer.session(suffix);}
    JsonObject profile(){JsonObject b=object("{\"ok\":true,\"admin\":{\"accountId\":\""+ACTOR+"\",\"role\":\"dispatcher\",\"permissions\":[]}}");JsonObject a=b.getAsJsonObject("admin");if(read)a.getAsJsonArray("permissions").add("vehicles.read");if(verify)a.getAsJsonArray("permissions").add("vehicles.verify");
        if(owner||phoneScope){a.addProperty("role","owner");JsonArray all=new JsonArray();all.add("*");a.add("permissions",all);JsonObject proof=object("{\"required\":true,\"verified\":true,\"method\":\"totp\"}");proof.addProperty("verified",mfa);proof.addProperty("expiresAt",System.currentTimeMillis()+3600000);a.add("mfa",proof);}if(phoneScope)a.addProperty("testScope","owner_phone");return b;}
    JsonObject vehicle(String id){JsonObject v=object("{\"id\":\""+id+"\",\"driverId\":\""+DRIVER+"\",\"make\":\"Haval\",\"model\":\"F7\",\"plate\":\"SYNTH-01\",\"status\":\"approved\",\"revision\":7,\"lastPhotoCheckPassedAt\":null,\"nextPhotoCheckDueAt\":null}");v.addProperty("revision",revision);return v;}
    JsonObject check(){JsonObject c=object("{\"id\":\"photo-check_native001\",\"vehicleId\":\""+CAR+"\",\"kind\":\"manual\",\"restrictionMode\":\"after_current_trip\",\"result\":\"pending\",\"revision\":1,\"dueAt\":1800000000000,\"graceUntil\":1800000000000,\"createdAt\":1800000000000,\"updatedAt\":1800000000000}");if(!lastBody.isEmpty()){JsonObject sent=object(lastBody);c.addProperty("kind",sent.get("kind").getAsString());c.addProperty("restrictionMode",sent.get("restrictionMode").getAsString());}return c;}
    private Response reply(Chain c,int status,JsonObject body){body.addProperty("requestId","req_native001");return new Response.Builder().request(c.request()).protocol(Protocol.HTTP_1_1).code(status).message("Synthetic").body(ResponseBody.create(MediaType.get("application/json"),body.toString())).build();}
    @Override public Response intercept(Chain chain)throws IOException {
        assertEquals("Bearer synthetic-original",chain.request().header("Authorization"));String path=chain.request().url().encodedPath();
        if(path.equals(holdPath)&&entered!=null){entered.countDown();try{assertTrue(release.await(8,TimeUnit.SECONDS));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Synthetic cancelled");}}
        if(path.equals("/v1/admin/me"))return reply(chain,200,profile());
        if(path.equals("/v1/admin/vehicles")){vehicles.incrementAndGet();assertEquals("20",chain.request().url().queryParameter("limit"));JsonObject b=object("{\"ok\":true,\"vehicles\":[],\"nextCursor\":null}");b.getAsJsonArray("vehicles").add(vehicle(chain.request().url().queryParameter("cursor")==null?CAR:CAR2));if(paginate&&chain.request().url().queryParameter("cursor")==null)b.addProperty("nextCursor","page_native2");return reply(chain,200,b);}
        assertEquals("/v1/admin/vehicles/"+CAR+"/photo-checks",path);
        if("GET".equals(chain.request().method())){JsonObject b=object("{\"ok\":true,\"vehicleId\":\""+CAR+"\",\"vehicleRevision\":7,\"checks\":[],\"nextCursor\":null}");b.addProperty("vehicleRevision",revision);if(commits.get()>0)b.getAsJsonArray("checks").add(check());return reply(chain,200,b);}
        assertEquals("POST",chain.request().method());assertTrue(chain.request().body().isOneShot());commands.incrementAndGet();okio.Buffer bytes=new okio.Buffer();chain.request().body().writeTo(bytes);String raw=bytes.readUtf8(),key=chain.request().header("Idempotency-Key");JsonObject body=object(raw);assertEquals(4,body.size());assertTrue(key.startsWith("photo-control-"));
        if(failure!=0)return reply(chain,failure,object("{\"ok\":false,\"code\":\"synthetic_failure\",\"error\":\"PRIVATE_PROVIDER\"}")).newBuilder().header("Retry-After","0").build();
        if(key.equals(lastKey)){assertEquals(lastBody,raw);return reply(chain,201,cachedAck);}
        assertEquals(revision,body.get("expectedRevision").getAsLong());lastKey=key;lastBody=raw;commits.incrementAndGet();revision++;
        JsonObject c=check();c.addProperty("kind",body.get("kind").getAsString());c.addProperty("restrictionMode",body.get("restrictionMode").getAsString());c.addProperty("vehicleRevision",revision);cachedAck=object("{\"ok\":true}");cachedAck.add("photoCheck",c);
        if(lostAck){lostAck=false;throw new IOException("Synthetic lost acknowledgement");}return reply(chain,201,cachedAck);
    }
}
