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
        Failure(int status) { super("Admin request failed"); this.status = status; }
    }
    public AdminApiClient() { client = AdminEndpointPolicy.clientBuilder().build(); }
    // Package-private transport seam for offline protocol tests. The origin guard always runs first.
    AdminApiClient(Interceptor transport) { client = AdminEndpointPolicy.clientBuilder().addInterceptor(transport).build(); }
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
        return request("GET", "v1/admin/registrations?limit=50", null, accessToken);
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
        Request.Builder builder = new Request.Builder().url(AdminEndpointPolicy.BASE_URL + path)
                .header("Accept", "application/json").header("Cache-Control", "no-store");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        builder.method(method, body == null ? null : RequestBody.create(JSON, body.toString()));
        try (Response response = client.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) throw new Failure(response.code());
            ResponseBody responseBody = response.body();
            if (responseBody == null || responseBody.contentLength() > MAX_RESPONSE_BYTES) throw new IOException("Invalid response");
            responseBody.source().request(MAX_RESPONSE_BYTES + 1);
            if (responseBody.source().getBuffer().size() > MAX_RESPONSE_BYTES) throw new IOException("Response too large");
            try { return JsonParser.parseString(responseBody.string()).getAsJsonObject(); }
            catch (RuntimeException invalid) { throw new IOException("Invalid response"); }
        }
    }
}
