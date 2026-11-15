package ru.transferservis.admin.data;

import android.graphics.Bitmap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;

/** Offline client fixture only. Actual backend SQL and authorization have separate D1 tests. */
final class ModerationServer implements Interceptor {
    static final String DRIVER="staff_driver123",DOCUMENT="document_fixture001",NAME="Synthetic Moderation Driver";
    volatile String status="submitted",documentStatus="pending";
    volatile boolean reviewGrant=true,sensitiveGrant=true,dropAcknowledgement=false;
    final AtomicInteger posts=new AtomicInteger(),resources=new AtomicInteger();
    final Map<String,String> completed=new HashMap<>();
    final JsonArray history=new JsonArray();
    volatile String lastKey="",lastBody="";
    long revision=1,documentRevision=1;
    AdminSession session(){long now=System.currentTimeMillis();return new AdminSession("admin-1","Fixture Admin","fixture-access",now+600000,"fixture-refresh",now+1200000);}
    JsonObject profile(){JsonObject a=new JsonObject();a.addProperty("accountId","admin-1");a.addProperty("role","dispatcher");JsonArray permissions=new JsonArray();permissions.add("drivers.read");permissions.add("drivers.verify_identity");permissions.add("vehicles.verify");if(reviewGrant)permissions.add("drivers.verify_documents");if(sensitiveGrant)permissions.add("documents.read_sensitive");a.add("permissions",permissions);JsonObject r=new JsonObject();r.addProperty("ok",true);r.add("admin",a);return r;}
    JsonObject card(){
        JsonObject profile=new JsonObject();profile.addProperty("driverId",DRIVER);profile.addProperty("fullName",NAME);profile.addProperty("region","Synthetic Region");profile.addProperty("birthDate","1990-01-01");profile.addProperty("registrationStatus",status);profile.addProperty("identityStatus","pending");profile.addProperty("revision",revision);
        JsonObject body=new JsonObject();body.add("profile",profile);body.add("vehicles",new JsonArray());body.add("photos",new JsonArray());body.add("reviews",history.deepCopy());JsonArray documents=new JsonArray();
        for(int n=1;n<=2;n++){JsonObject item=new JsonObject();item.addProperty("id",n==1?DOCUMENT:"document_fixture002");item.addProperty("kind",n==1?"passport_main":"driver_license_front");item.addProperty("status",n==1?documentStatus:"verified");item.addProperty("revision",n==1?documentRevision:1);documents.add(item);}body.add("documents",documents);
        JsonObject response=new JsonObject();response.addProperty("ok",true);response.add("registration",body);return response;
    }
    private Response json(Request r,int status,String body){return new Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(status).message("fixture").body(ResponseBody.create(MediaType.get("application/json"),body)).build();}
    @Override public synchronized Response intercept(Chain chain) throws IOException {
        Request request=chain.request();String path=request.url().encodedPath();
        if(path.equals("/staff/login"))return json(request,200,session().toResponse().toString());
        if(path.equals("/staff/logout"))return json(request,200,"{\"ok\":true}");
        if(path.equals("/v1/admin/me"))return json(request,200,profile().toString());
        if(path.equals("/v1/admin/registrations"))return json(request,200,"{\"ok\":true,\"registrations\":[{\"driverId\":\""+DRIVER+"\",\"fullName\":\""+NAME+"\",\"registrationStatus\":\""+status+"\"}]}");
        if(path.endsWith("/resources/document/"+DOCUMENT)){
            resources.incrementAndGet();Bitmap bitmap=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);ByteArrayOutputStream bytes=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.PNG,100,bytes);bitmap.recycle();
            return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("fixture").body(ResponseBody.create(MediaType.get("image/png"),bytes.toByteArray())).build();
        }
        if("GET".equals(request.method()))return json(request,200,card().toString());
        posts.incrementAndGet();String key=request.header("Idempotency-Key");lastKey=key;okio.Buffer body=new okio.Buffer();request.body().writeTo(body);lastBody=body.readUtf8();
        if(completed.containsKey(key))return json(request,200,completed.get(key));
        JsonObject input=com.google.gson.JsonParser.parseString(lastBody).getAsJsonObject();String type,id,decision;
        if(path.endsWith("/review")){status="under_review";revision++;type="registration";id=DRIVER;decision="under_review";}
        else if(path.contains("/items/document/")){documentStatus=input.get("decision").getAsString();documentRevision++;type="document";id=DOCUMENT;decision=documentStatus;}
        else {status=input.get("decision").getAsString();revision++;type="registration";id=DRIVER;decision=status;}
        JsonObject event=new JsonObject();event.addProperty("targetType",type);event.addProperty("targetId",id);event.addProperty("decision",decision);event.addProperty("note",AdminSession.string(input,"note"));event.addProperty("createdAt",System.currentTimeMillis());history.add(event);
        completed.put(key,"{\"ok\":true}");if(dropAcknowledgement){dropAcknowledgement=false;throw new IOException("Synthetic lost acknowledgement");}
        return json(request,200,"{\"ok\":true}");
    }
}
