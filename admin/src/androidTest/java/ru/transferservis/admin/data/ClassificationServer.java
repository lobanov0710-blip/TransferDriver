package ru.transferservis.admin.data;
import com.google.gson.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import static org.junit.Assert.*;
final class ClassificationServer implements Interceptor {
    static final String ACTOR=PhotoControlServer.ACTOR,CAR=PhotoControlServer.CAR;
    final PhotoControlServer auth=new PhotoControlServer();
    final AtomicInteger reads=new AtomicInteger(),rules=new AtomicInteger(),requests=new AtomicInteger();
    volatile boolean unverified,paginate;volatile long revision=4,mfaLifetime=3600000;volatile int failure;volatile String holdPath="";
    CountDownLatch entered,release;
    volatile String bodyType="sedan";
    static AdminSession session(String suffix){return PhotoControlServer.session(suffix);}
    static JsonObject json(String s){return JsonParser.parseString(s).getAsJsonObject();}
    JsonObject metadata(boolean next){
        JsonObject b=json("{\"ok\":true,\"vehicleId\":\""+CAR+"\",\"vehicleRevision\":4,\"inputRevision\":1,\"declaredParameters\":{\"make\":\"Toyota\",\"model\":\"Camry\",\"modelYear\":2023,\"color\":\"Белый\"},\"verifiedParameters\":null,\"pinnedRuleVersion\":\"rule_native001\",\"classification\":null,\"evidence\":[],\"conditions\":[{\"id\":\"rule_native001:1\",\"text\":\"Салон в хорошем состоянии\"}],\"history\":[],\"nextHistoryCursor\":null}");b.addProperty("vehicleRevision",revision);
        if(!unverified){b.add("verifiedParameters",json("{\"make\":\"Toyota\",\"model\":\"Camry\",\"modelYear\":2023,\"powertrain\":\"hybrid\",\"bodyType\":\"sedan\"}"));b.add("classification",json("{\"outcome\":\"candidate\",\"vehicleClass\":\"comfort\",\"ruleVersion\":\"rule_native001\",\"reasonCodes\":[]}"));
            b.getAsJsonObject("verifiedParameters").addProperty("bodyType",bodyType);
            for(String id:new String[]{"document_front001","document_back0001"})b.getAsJsonArray("evidence").add(json("{\"documentId\":\""+id+"\",\"expectedRevision\":1}"));}
        else{b.getAsJsonObject("declaredParameters").add("modelYear",JsonNull.INSTANCE);b.getAsJsonObject("declaredParameters").addProperty("color","\u00a0\uFEFF");}
        int size=paginate&&!next?50:1;for(int i=0;i<size;i++)b.getAsJsonArray("history").add(json("{\"assessmentId\":\"assessment_native"+(next?"old":i)+"\",\"vehicleRevision\":2,\"ruleVersion\":\"rule_native001\",\"outcome\":\"candidate\",\"createdAt\":1800000000000}"));
        if(paginate&&!next)b.addProperty("nextHistoryCursor","history_native2");return b;
    }
    JsonObject rule(){JsonObject b=json("{\"ok\":true,\"rule\":{\"version\":\"rule_native001\",\"make\":\"Toyota\",\"model\":\"Camry\",\"revision\":1,\"minYear\":2020,\"vehicleClass\":\"comfort\",\"allowedBodyTypes\":[],\"conditions\":[{\"id\":\"rule_native001:1\",\"text\":\"Салон в хорошем состоянии\"}]}}");b.getAsJsonObject("rule").getAsJsonArray("allowedBodyTypes").add(bodyType);return b;}
    private Response reply(Chain chain,int code,JsonObject body){body.addProperty("requestId","req_native001");return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("Synthetic").header("Cache-Control","no-store").header("Retry-After","0").body(ResponseBody.create(MediaType.get("application/json"),body.toString())).build();}
    @Override public Response intercept(Chain chain)throws IOException {
        requests.incrementAndGet();assertEquals("GET",chain.request().method());assertEquals("Bearer synthetic-original",chain.request().header("Authorization"));assertNull(chain.request().header("Idempotency-Key"));
        String path=chain.request().url().encodedPath();
        if(path.equals(holdPath)&&entered!=null){entered.countDown();try{assertTrue(release.await(8,TimeUnit.SECONDS));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Synthetic cancelled");}}
        if(path.equals("/v1/admin/me")){JsonObject profile=auth.profile();if(profile.getAsJsonObject("admin").has("mfa"))profile.getAsJsonObject("admin").getAsJsonObject("mfa").addProperty("expiresAt",System.currentTimeMillis()+mfaLifetime);return reply(chain,200,profile);}
        if(path.equals("/v1/admin/vehicle-rules/versions/rule_native001")){rules.incrementAndGet();return reply(chain,200,rule());}
        assertEquals("/v1/admin/vehicles/"+CAR+"/classification",path);reads.incrementAndGet();
        if(failure!=0)return reply(chain,failure,json("{\"ok\":false,\"code\":\"synthetic_failure\",\"error\":\"PRIVATE_PROVIDER_SQL\"}"));
        return reply(chain,200,metadata(chain.request().url().queryParameter("historyCursor")!=null));
    }
}
