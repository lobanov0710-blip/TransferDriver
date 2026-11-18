package ru.transferservis.admin.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import ru.transferservis.admin.security.AdminEndpointPolicy;

public final class AdminApiClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final long MAX_RESPONSE_BYTES = 1_048_576;
    private final OkHttpClient client;
    public static final class Failure extends IOException {
        public final int status;
        public final String code;
        Failure(int status) { this(status, ""); }
        Failure(int status,String code) { super("Admin request failed"); this.status = status; this.code=code; }
    }
    public AdminApiClient() { this(null,null); }
    public void cancelPending() { client.dispatcher().cancelAll(); }
    // Package-private transport seam for offline protocol tests. The origin guard always runs first.
    AdminApiClient(Interceptor transport) { this(null,transport); }
    AdminApiClient(Interceptor guard,Interceptor transport) {
        OkHttpClient.Builder builder=AdminEndpointPolicy.clientBuilder();if(guard!=null)builder.addInterceptor(guard);if(transport!=null)builder.addInterceptor(transport);client=builder.build();
    }
    JsonObject assignmentMe(String token,long scope)throws IOException {return request("GET","v1/admin/me",null,token,null,scope);}
    JsonObject classification(String token,String vehicle,String cursor,long scope)throws IOException {
        String next=VehicleClassification.cursor(cursor);
        return request("GET","v1/admin/vehicles/"+Assignments.vehicleId(vehicle)+"/classification"+(next.isEmpty()?"":"?historyCursor="+next),null,token,null,scope);
    }
    JsonObject classificationRule(String token,String version,long scope)throws IOException {
        return request("GET","v1/admin/vehicle-rules/versions/"+VehicleClassification.version(version),null,token,null,scope);
    }
    JsonObject classificationRules(String token,String make,String model,long scope)throws IOException {
        okhttp3.HttpUrl url=okhttp3.HttpUrl.get(AdminEndpointPolicy.BASE_URL+"v1/admin/vehicle-rules").newBuilder().addQueryParameter("make",make).addQueryParameter("model",model).addQueryParameter("limit","1").build();
        return request("GET",url.toString().substring(AdminEndpointPolicy.BASE_URL.length()),null,token,null,scope);
    }
    JsonObject classificationRegistration(String token,String driver,long scope)throws IOException {return request("GET","v1/admin/registrations/"+RegistrationReview.driverId(driver),null,token,null,scope);}
    JsonObject classificationReview(String token,VehicleAssessmentAction action)throws IOException {return request("POST","v1/admin/vehicles/"+Assignments.vehicleId(action.context.vehicleId)+"/classification/review",action.body(),token,action.key,action.scope);}
    JsonObject photoVehicles(String token,String status,String cursor,long scope)throws IOException {
        if(status==null||(!status.isEmpty()&&!PhotoControl.STATUSES.contains(status)))throw new IOException("Invalid filter");
        String next=Assignments.cursor(cursor);return request("GET","v1/admin/vehicles?limit=20"+(status.isEmpty()?"":"&status="+status)+(next.isEmpty()?"":"&cursor="+next),null,token,null,scope);
    }
    JsonObject photoChecks(String token,String vehicle,String cursor,long scope)throws IOException {
        String next=Assignments.cursor(cursor);return request("GET","v1/admin/vehicles/"+Assignments.vehicleId(vehicle)+"/photo-checks?limit=20"+(next.isEmpty()?"":"&cursor="+next),null,token,null,scope);
    }
    JsonObject photoCommand(String token,PhotoCheckAction action)throws IOException {
        return request("POST","v1/admin/vehicles/"+Assignments.vehicleId(action.vehicle.id)+"/photo-checks",action.body(),token,action.key,action.scope);
    }
    JsonObject assignmentOrders(String token,String status,String cursor,long scope)throws IOException {
        if(!java.util.Arrays.asList("","new","taken","in_progress").contains(status))throw new IOException("Invalid filter");
        String next=Assignments.cursor(cursor);return request("GET","v1/admin/orders?limit=20"+(status.isEmpty()?"":"&status="+status)+(next.isEmpty()?"":"&cursor="+next),null,token,null,scope);
    }
    JsonObject assignmentOptions(String token,String id,String cursor,long scope)throws IOException {
        String next=Assignments.cursor(cursor);return request("GET","v1/admin/orders/"+Assignments.orderId(id)+"/assignment-options?limit=20"+(next.isEmpty()?"":"&cursor="+next),null,token,null,scope);
    }
    JsonObject assignmentCommand(String token,AssignmentAction action)throws IOException {
        return request("POST","v1/admin/orders/"+Assignments.orderId(action.order.id)+(action.reassign?"/reassign":"/assign"),action.body(),token,action.key,action.scope);
    }
    public JsonObject login(String login, String password) throws IOException {
        JsonObject body = new JsonObject(); body.addProperty("login", login); body.addProperty("password", password);
        return request("POST", "staff/login", body, null);
    }
    public JsonObject refresh(String refreshToken) throws IOException {
        return request("POST", "staff/refresh", tokenBody(refreshToken), null);
    }
    public void logout(String refreshToken) throws IOException {
        request("POST", "staff/logout", tokenBody(refreshToken), null);
    }
    public JsonObject me(String accessToken) throws IOException {
        return request("GET", "v1/admin/me", null, accessToken);
    }
    public JsonObject registrations(String accessToken) throws IOException {
        return registrations(accessToken, "");
    }
    public JsonObject registrations(String accessToken,String status) throws IOException {
        if(!status.isEmpty()&&(!RegistrationReview.isRegistrationStatus(status)||"draft".equals(status)))throw new IOException("Invalid filter");
        return request("GET", "v1/admin/registrations?limit=50"+(status.isEmpty()?"":"&status="+status), null, accessToken);
    }
    public JsonObject review(String token,String driverId) throws IOException {
        return request("GET", "v1/admin/registrations/"+RegistrationReview.driverId(driverId), null, token);
    }
    public void reviewAction(String token,ReviewAction action) throws IOException {
        String path="v1/admin/registrations/"+RegistrationReview.driverId(action.driverId)+"/review";
        if("complete".equals(action.type))path+="/complete";
        else if(!"start".equals(action.type))path+="/items/"+action.type+"/"+RegistrationReview.targetId(action.targetId);
        JsonObject response=request("POST",path,action.body(),token,action.key);
        if(!OwnerMfa.trueField(response,"ok"))throw new IOException("Invalid mutation response");
    }
    public byte[] resource(String token,String driverId,RegistrationReview.Item item) throws IOException {
        return resource(token,driverId,item,(Long)null);
    }
    byte[] resource(String token,String driverId,RegistrationReview.Item item,long scope)throws IOException {return resource(token,driverId,item,Long.valueOf(scope));}
    private byte[] resource(String token,String driverId,RegistrationReview.Item item,Long scope)throws IOException {
        if(!"document".equals(item.type)&&!"vehicle_photo".equals(item.type))throw new IOException("Invalid resource");
        Request request=new Request.Builder().url(AdminEndpointPolicy.BASE_URL+"v1/admin/registrations/"
                +RegistrationReview.driverId(driverId)+"/resources/"+item.type+"/"+RegistrationReview.targetId(item.id))
                .tag(Long.class,scope).header("Authorization","Bearer "+token).header("Cache-Control","no-store").header("Accept","image/jpeg, image/png, image/webp").build();
        try(Response response=client.newCall(request).execute()) {
            if(!response.isSuccessful())throw failure(response);
            ResponseBody body=response.body();
            String type=body==null||body.contentType()==null?"":body.contentType().type()+"/"+body.contentType().subtype();
            if(!java.util.Arrays.asList("image/jpeg","image/png","image/webp").contains(type))throw new IOException("Unsupported resource");
            return bounded(body,10_485_760);
        }
    }
    public JsonObject verifyMfa(String challengeId, String code, boolean recovery) throws IOException {
        JsonObject body = new JsonObject(); body.addProperty("challengeId", challengeId);
        body.addProperty(recovery ? "recoveryCode" : "code", code);
        return request("POST", recovery ? "v1/admin/mfa/recovery/verify" : "v1/admin/mfa/totp/verify", body, null);
    }
    public JsonObject startEnrollment(String challengeId, String bootstrapCode) throws IOException {
        JsonObject body = new JsonObject(); body.addProperty("challengeId", challengeId); body.addProperty("bootstrapCode", bootstrapCode);
        return request("POST", "v1/admin/mfa/enrollment/start", body, null);
    }
    public JsonObject confirmEnrollment(String enrollmentId, String code) throws IOException {
        JsonObject body = new JsonObject(); body.addProperty("enrollmentId", enrollmentId); body.addProperty("code", code);
        return request("POST", "v1/admin/mfa/enrollment/confirm", body, null);
    }
    private static JsonObject tokenBody(String token) {
        JsonObject body = new JsonObject(); body.addProperty("refreshToken", token); return body;
    }
    private JsonObject request(String method, String path, JsonObject body, String token) throws IOException {
        return request(method,path,body,token,null);
    }
    private JsonObject request(String method, String path, JsonObject body, String token,String key) throws IOException {
        return request(method,path,body,token,key,null);
    }
    private JsonObject request(String method,String path,JsonObject body,String token,String key,Long scope)throws IOException {
        Request.Builder builder = new Request.Builder().url(AdminEndpointPolicy.BASE_URL + path).tag(Long.class,scope)
                .header("Accept", "application/json").header("Cache-Control", "no-store");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if(key!=null)builder.header("Idempotency-Key",key);
        builder.method(method, body == null ? null : RequestBody.create(JSON, body.toString()));
        try (Response response = client.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) throw failure(response);
            ResponseBody responseBody = response.body();
            if (responseBody == null || responseBody.contentLength() > MAX_RESPONSE_BYTES) throw new IOException("Invalid response");
            responseBody.source().request(MAX_RESPONSE_BYTES + 1);
            if (responseBody.source().getBuffer().size() > MAX_RESPONSE_BYTES) throw new IOException("Response too large");
            try { return JsonParser.parseString(responseBody.string()).getAsJsonObject(); }
            catch (RuntimeException invalid) { throw new IOException("Invalid response"); }
        }
    }
    private static byte[] bounded(ResponseBody body,long maximum) throws IOException {
        if(body==null||body.contentLength()>maximum)throw new IOException("Invalid response");
        body.source().request(maximum+1);if(body.source().getBuffer().size()>maximum)throw new IOException("Response too large");
        return body.bytes();
    }
    private static Failure failure(Response response) throws IOException {
        String code="";ResponseBody body=response.body();
        if(body!=null&&body.contentLength()<=4096){
            body.source().request(4097);
            if(body.source().getBuffer().size()<=4096)try {
                String value=AdminSession.string(JsonParser.parseString(body.string()).getAsJsonObject(),"code");
                if(value.matches("[a-z0-9_]{1,80}"))code=value;
            }catch(RuntimeException ignored){/* Raw server error text is never exposed or logged. */}
        }
        return new Failure(response.code(),code);
    }
}
