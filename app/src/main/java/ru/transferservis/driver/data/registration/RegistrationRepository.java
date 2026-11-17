package ru.transferservis.driver.data.registration;

import android.content.Context;
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
import ru.transferservis.driver.data.api.SecureHttpClient;
import ru.transferservis.driver.data.session.DriverSession;
import ru.transferservis.driver.data.session.SecureSessionStore;
import ru.transferservis.driver.security.ApiEndpointPolicy;

/** Explicit user mutations: no interceptor refresh, retry or account substitution. */
public final class RegistrationRepository {
    public static final class Failure extends IOException {
        public final int status;
        public final String code;
        public Failure(int status, String code) { super("Registration request failed"); this.status = status; this.code = code; }
    }
    private final SecureSessionStore store;
    private final OkHttpClient client;
    private DriverSession bound;
    private long epoch;
    public RegistrationRepository(Context context) { this(context, null); }
    // Offline test transport runs after the production origin/TLS policy. No runtime hook.
    RegistrationRepository(Context context, Interceptor transport) {
        store = new SecureSessionStore(context);
        OkHttpClient.Builder builder = SecureHttpClient.newBuilder();
        builder.addInterceptor(chain -> {
            Long scope = chain.request().tag(Long.class);
            if (scope == null) throw new Failure(401, "session_changed");
            DriverSession session = requireCurrent(scope);
            if (!("Bearer " + session.getAccessToken()).equals(chain.request().header("Authorization")))
                throw new Failure(401, "session_changed");
            return chain.proceed(chain.request());
        });
        if (transport != null) builder.addInterceptor(transport);
        client = builder.build();
    }
    public synchronized void cancel() { epoch++; bound = null; client.dispatcher().cancelAll(); }
    public synchronized long bind() throws IOException {
        DriverSession current = store.load();
        if (current == null || !current.isDriver() || !current.hasUsableAccessToken() || !current.hasUsableRefreshToken())
            throw new Failure(401, "session_required");
        bound = current; return ++epoch;
    }
    public synchronized boolean current(long scope) {
        DriverSession actual = store.load();
        return scope == epoch && bound != null && actual != null && actual.isDriver() && actual.hasUsableAccessToken()
                && actual.hasUsableRefreshToken() && bound.getStaffId().equals(actual.getStaffId())
                && bound.getRefreshToken().equals(actual.getRefreshToken()) && bound.getAccessToken().equals(actual.getAccessToken());
    }
    public Registration load(long scope) throws IOException {
        DriverSession session = requireCurrent(scope);
        return Registration.parse(request("GET", "v1/driver/me", null, session, scope), session.getStaffId());
    }
    public void saveProfile(long scope, JsonObject body) throws IOException { mutate("PUT", "v1/driver/registration", body, scope); }
    public void availability(long scope, String vehicleId, String state, long revision, String key) throws IOException {
        if (revision < 1 || !("on_line".equals(state) || "off_line".equals(state))
                || ("on_line".equals(state) && (vehicleId == null || vehicleId.isEmpty()))
                || (vehicleId != null && !vehicleId.matches("vehicle_[A-Za-z0-9_-]{8,100}"))
                || key == null || !key.matches("availability-[0-9a-f-]{36}")) throw new IOException("Invalid availability operation");
        JsonObject body = new JsonObject();
        body.addProperty("activeVehicleId", vehicleId); body.addProperty("requestedState", state); body.addProperty("expectedRevision", revision);
        JsonObject result = request("PUT", "v1/driver/availability", body, requireCurrent(scope), scope, key);
        if (!result.has("ok") || !result.get("ok").isJsonPrimitive() || !result.get("ok").getAsJsonPrimitive().isBoolean()
                || !result.get("ok").getAsBoolean()) throw new IOException("Invalid availability response");
    }
    public boolean saveVehicle(long scope, String id, JsonObject body) throws IOException {
        if (id != null && !id.matches("vehicle_[0-9a-fA-F-]{36}")) throw new IOException("Invalid vehicle ID");
        JsonObject result = mutate(id == null ? "POST" : "PATCH", "v1/driver/vehicles" + (id == null ? "" : "/" + id), body, scope);
        if (!result.has("requiresDispatcherReassignment")) return false;
        if (!result.get("requiresDispatcherReassignment").isJsonPrimitive()
                || !result.get("requiresDispatcherReassignment").getAsJsonPrimitive().isBoolean())
            throw new IOException("Invalid mutation response");
        return result.get("requiresDispatcherReassignment").getAsBoolean();
    }
    private JsonObject mutate(String method, String path, JsonObject body, long scope) throws IOException {
        JsonObject result = request(method, path, body, requireCurrent(scope), scope);
        if (!result.has("ok") || !result.get("ok").isJsonPrimitive() || !result.get("ok").getAsJsonPrimitive().isBoolean()
                || !result.get("ok").getAsBoolean()) throw new IOException("Invalid mutation response");
        return result;
    }
    private synchronized DriverSession requireCurrent(long scope) throws IOException {
        if (!current(scope)) throw new Failure(401, "session_changed"); return bound;
    }
    private JsonObject request(String method, String path, JsonObject body, DriverSession session, long scope) throws IOException {
        return request(method, path, body, session, scope, null);
    }
    private JsonObject request(String method, String path, JsonObject body, DriverSession session, long scope, String key) throws IOException {
        Request.Builder builder = new Request.Builder().url(ApiEndpointPolicy.BASE_URL + path).tag(Long.class, scope)
                .header("Authorization", "Bearer " + session.getAccessToken()).header("Accept", "application/json")
                .header("Cache-Control", "no-store")
                .method(method, body == null ? null : RequestBody.create(MediaType.get("application/json; charset=utf-8"), body.toString()));
        if (key != null) builder.header("Idempotency-Key", key);
        Request request = builder.build();
        // A queued request must still belong to the same session at dispatch time.
        requireCurrent(scope);
        try (Response response = client.newCall(request).execute()) {
            requireCurrent(scope);
            if (!response.isSuccessful()) throw failure(response);
            ResponseBody responseBody = response.body();
            if (responseBody == null || responseBody.contentLength() > 1_048_576) throw new IOException("Invalid response");
            responseBody.source().request(1_048_577);
            if (responseBody.source().getBuffer().size() > 1_048_576) throw new IOException("Response too large");
            try { JsonObject result = JsonParser.parseString(responseBody.string()).getAsJsonObject(); requireCurrent(scope); return result; }
            catch (RuntimeException malformed) { throw new IOException("Invalid response"); }
        }
    }
    private static Failure failure(Response response) throws IOException {
        String code = ""; ResponseBody body = response.body();
        if (body != null && body.contentLength() <= 4096) {
            body.source().request(4097);
            if (body.source().getBuffer().size() <= 4096) try {
                String candidate = JsonParser.parseString(body.string()).getAsJsonObject().get("code").getAsString();
                if (candidate.matches("[a-z0-9_]{1,80}")) code = candidate;
            } catch (RuntimeException ignored) { /* Provider text is not shown or logged. */ }
        }
        return new Failure(response.code(), code);
    }
}
