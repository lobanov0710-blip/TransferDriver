package ru.transferservis.admin.data;
import com.google.gson.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import static org.junit.Assert.*;
/** Synthetic wire fixture; backend SQL/CAS has separate native D1 coverage. */
final class AssessmentServer implements Interceptor {
    static final String ACTOR=ClassificationServer.ACTOR,CAR=ClassificationServer.CAR,DRIVER=ModerationServer.DRIVER;
    final ClassificationServer source=new ClassificationServer();final ModerationServer registration=new ModerationServer();
    final AtomicInteger commands=new AtomicInteger(),commits=new AtomicInteger(),reads=new AtomicInteger(),ruleReads=new AtomicInteger(),resources=new AtomicInteger();
    boolean sensitive=true,lostAck,wrongAck;int failure;String lastKey="",lastBody="",holdPath="";JsonObject cached;CountDownLatch entered,release;
    AssessmentServer(){registration.documentStatus="verified";}
    static JsonObject json(String s){return ClassificationServer.json(s);}
    JsonObject profile(){JsonObject b=source.auth.profile();JsonObject a=b.getAsJsonObject("admin");if(sensitive){a.getAsJsonArray("permissions").add("documents.read_sensitive");a.getAsJsonArray("permissions").add("drivers.verify_documents");}if(a.has("mfa"))a.getAsJsonObject("mfa").addProperty("expiresAt",System.currentTimeMillis()+source.mfaLifetime);return b;}
    JsonObject card(){JsonObject b=registration.card(),r=b.getAsJsonObject("registration");r.getAsJsonArray("vehicles").add(json("{\"id\":\""+CAR+"\",\"make\":\"Toyota\",\"model\":\"Camry\",\"plate\":\"SYNTH-01\",\"revision\":"+source.revision+"}"));r.add("documents",new JsonArray());for(String side:new String[]{"front","back"})r.getAsJsonArray("documents").add(json("{\"id\":\"document_"+side+"001\",\"vehicleId\":\""+CAR+"\",\"kind\":\"sts_"+side+"\",\"status\":\"verified\",\"revision\":1}"));return b;}
    private Response reply(Chain c,int status,JsonObject b){b.addProperty("requestId","req_native001");return new Response.Builder().request(c.request()).protocol(Protocol.HTTP_1_1).code(status).message("Synthetic").header("Retry-After","0").body(ResponseBody.create(MediaType.get("application/json"),b.toString())).build();}
    @Override public Response intercept(Chain c)throws IOException {
        Request req=c.request();assertEquals("Bearer synthetic-original",req.header("Authorization"));String path=req.url().encodedPath();
        if(path.equals(holdPath)&&entered!=null){entered.countDown();try{assertTrue(release.await(8,TimeUnit.SECONDS));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Synthetic cancelled");}}
        if(path.equals("/v1/admin/me")){assertEquals("GET",req.method());return reply(c,200,profile());}
        if(path.equals("/v1/admin/registrations/"+DRIVER)){assertEquals("GET",req.method());reads.incrementAndGet();return reply(c,200,card());}
        if(path.equals("/v1/admin/vehicles/"+CAR+"/classification")){assertEquals("GET",req.method());reads.incrementAndGet();return reply(c,200,source.metadata(false));}
        if(path.equals("/v1/admin/vehicle-rules")){assertEquals("GET",req.method());ruleReads.incrementAndGet();assertEquals("Toyota",req.url().queryParameter("make"));assertEquals("Camry",req.url().queryParameter("model"));assertEquals("1",req.url().queryParameter("limit"));JsonObject rule=source.rule().getAsJsonObject("rule").deepCopy();rule.addProperty("version","rule_current001");rule.getAsJsonArray("conditions").get(0).getAsJsonObject().addProperty("id","rule_current001:1");JsonObject b=json("{\"ok\":true,\"rules\":[],\"nextCursor\":null}");b.getAsJsonArray("rules").add(rule);return reply(c,200,b);}
        if(path.startsWith("/v1/admin/vehicle-rules/versions/")){assertEquals("/v1/admin/vehicle-rules/versions/rule_native001",path);ruleReads.incrementAndGet();return reply(c,200,source.rule());}
        if(path.equals("/v1/admin/registrations/"+DRIVER+"/resources/document/document_front001")){resources.incrementAndGet();android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(2,2,android.graphics.Bitmap.Config.ARGB_8888);java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,bytes);image.recycle();return new Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(200).message("Synthetic").body(ResponseBody.create(MediaType.get("image/png"),bytes.toByteArray())).build();}
        assertEquals("/v1/admin/vehicles/"+CAR+"/classification/review",path);assertEquals("POST",req.method());assertTrue(req.body().isOneShot());commands.incrementAndGet();okio.Buffer bytes=new okio.Buffer();req.body().writeTo(bytes);String raw=bytes.readUtf8(),key=req.header("Idempotency-Key");assertTrue(key.startsWith("vehicle-review-"));JsonObject body=json(raw);assertEquals(9,body.size());assertFalse(body.has("vehicleClass"));
        if(failure!=0)return reply(c,failure,json("{\"ok\":false,\"code\":\"synthetic_failure\",\"error\":\"PRIVATE_PROVIDER_SQL\"}"));
        if(key.equals(lastKey)){assertEquals(lastBody,raw);return reply(c,200,cached.deepCopy());}
        assertEquals(source.revision,body.get("expectedRevision").getAsLong());lastKey=key;lastBody=raw;commits.incrementAndGet();source.revision++;
        JsonObject decision=json("{\"outcome\":\"candidate\",\"vehicleClass\":\"comfort\",\"ruleVersion\":\"rule_native001\",\"reasonCodes\":[]}");if("exception".equals(body.get("mode").getAsString()))decision.addProperty("ruleVersion",body.getAsJsonObject("exception").get("basisRuleVersion").getAsString());else decision.add("ruleVersion",body.get("expectedRuleVersion"));
        JsonObject p=body.getAsJsonObject("parameters");boolean changed=p.get("modelYear").getAsLong()!=2023;cached=json("{\"ok\":true,\"vehicleId\":\""+CAR+"\",\"vehicleRevision\":"+source.revision+",\"inputRevision\":"+(changed?2:1)+",\"requiresDispatcherReassignment\":true}");cached.add("classification",decision);
        if(lostAck){lostAck=false;throw new IOException("Synthetic lost acknowledgement");}JsonObject result=cached.deepCopy();if(wrongAck)result.addProperty("vehicleRevision",99);return reply(c,200,result);
    }
}
