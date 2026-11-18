package ru.transferservis.admin.data;
import com.google.gson.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import static org.junit.Assert.*;
/** Offline rule API fixture. Actual atomic CAS/audit/authorization has separate D1 coverage. */
final class VehicleRuleServer implements Interceptor {
    static final String ACTOR=PhotoControlServer.ACTOR;final PhotoControlServer auth=new PhotoControlServer();
    final AtomicInteger reads=new AtomicInteger(),posts=new AtomicInteger(),commits=new AtomicInteger(),requests=new AtomicInteger();
    boolean exists=true,lostAck,wrongAck;int failure;long mfaLifetime=3600000;String lastBody="",lastKey="",holdPath="";JsonObject current,cached;CountDownLatch entered,release;
    VehicleRuleServer(){auth.owner=true;current=ClassificationServer.json("{\"version\":\"rule_native001\",\"make\":\"Toyota\",\"model\":\"Camry\",\"revision\":1,\"minYear\":2020,\"vehicleClass\":\"comfort\",\"allowedBodyTypes\":[\"sedan\",\"crossover\"],\"conditions\":[{\"id\":\"rule_native001:1\",\"text\":\"Original; intact condition\"}]}");}
    private Response reply(Chain c,int status,JsonObject b){b.addProperty("requestId","req_native001");return new Response.Builder().request(c.request()).protocol(Protocol.HTTP_1_1).code(status).message("Synthetic").header("Retry-After","0").header("Cache-Control","no-store").body(ResponseBody.create(MediaType.get("application/json"),b.toString())).build();}
    @Override public Response intercept(Chain c)throws IOException {requests.incrementAndGet();Request r=c.request();assertEquals("Bearer synthetic-original",r.header("Authorization"));String path=r.url().encodedPath();if(path.equals(holdPath)&&entered!=null){entered.countDown();try{assertTrue(release.await(8,TimeUnit.SECONDS));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Synthetic cancellation");}}
        if(path.equals("/v1/admin/me")){assertEquals("GET",r.method());JsonObject b=auth.profile();if(!auth.owner){b.getAsJsonObject("admin").getAsJsonArray("permissions").add("vehicle_rules.manage");b.getAsJsonObject("admin").getAsJsonArray("permissions").add("*");}if(b.getAsJsonObject("admin").has("mfa"))b.getAsJsonObject("admin").getAsJsonObject("mfa").addProperty("expiresAt",System.currentTimeMillis()+mfaLifetime);return reply(c,200,b);}
        assertEquals("/v1/admin/vehicle-rules",path);
        if("GET".equals(r.method())){reads.incrementAndGet();assertEquals("1",r.url().queryParameter("limit"));JsonObject b=ClassificationServer.json("{\"ok\":true,\"rules\":[],\"nextCursor\":null}");if(exists&&current.get("make").getAsString().equals(r.url().queryParameter("make"))&&current.get("model").getAsString().equals(r.url().queryParameter("model")))b.getAsJsonArray("rules").add(current.deepCopy());return reply(c,200,b);}
        assertEquals("POST",r.method());assertTrue(r.body().isOneShot());posts.incrementAndGet();okio.Buffer bytes=new okio.Buffer();r.body().writeTo(bytes);String raw=bytes.readUtf8(),key=r.header("Idempotency-Key");assertTrue(key.startsWith("vehicle-rule-"));JsonObject input=ClassificationServer.json(raw);assertEquals(8,input.size());assertFalse(input.has("actor"));
        if(failure!=0)return reply(c,failure,ClassificationServer.json("{\"ok\":false,\"code\":\"synthetic_failure\",\"error\":\"PRIVATE_PROVIDER_SQL\"}"));
        if(key.equals(lastKey)){assertEquals(raw,lastBody);JsonObject b=new JsonObject();b.addProperty("ok",true);b.add("rule",cached.deepCopy());return reply(c,201,b);}
        if(exists)assertEquals(current.get("version"),input.get("expectedCurrentVersion"));else assertTrue(input.get("expectedCurrentVersion").isJsonNull());lastBody=raw;lastKey=key;commits.incrementAndGet();JsonObject saved=new JsonObject();saved.addProperty("version","rule_created001");saved.addProperty("revision",exists?current.get("revision").getAsLong()+1:1);for(String name:new String[]{"make","model","minYear","vehicleClass","allowedBodyTypes"})saved.add(name,input.get(name));JsonArray conditions=new JsonArray();int i=0;for(JsonElement text:input.getAsJsonArray("conditions")){JsonObject item=new JsonObject();item.addProperty("id","rule_created001:"+(++i));item.add("text",text);conditions.add(item);}saved.add("conditions",conditions);current=saved;cached=saved.deepCopy();exists=true;
        if(lostAck){lostAck=false;throw new IOException("Synthetic lost acknowledgement");}JsonObject b=new JsonObject();b.addProperty("ok",true);b.add("rule",saved.deepCopy());if(wrongAck)b.getAsJsonObject("rule").addProperty("model","Foreign");return reply(c,201,b);
    }
}
