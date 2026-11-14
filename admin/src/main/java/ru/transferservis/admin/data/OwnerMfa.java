package ru.transferservis.admin.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** Volatile setup/login data only. Never serialize into preferences, saved state, logs or clipboard. */
public final class OwnerMfa {
    private OwnerMfa() {}
    static boolean trueField(JsonObject object, String key) {
        return object != null && object.has(key) && object.get(key).isJsonPrimitive()
                && object.getAsJsonPrimitive(key).isBoolean() && object.get(key).getAsBoolean();
    }
    static long timestamp(JsonObject object, String key) {
        try {
            if (!object.get(key).isJsonPrimitive() || !object.getAsJsonPrimitive(key).isNumber()) return -1;
            return object.get(key).getAsBigDecimal().longValueExact();
        } catch (RuntimeException invalid) { return -1; }
    }
    private static boolean tokenFree(JsonObject object) {
        return object != null && !object.has("accessToken") && !object.has("refreshToken");
    }
    public static final class Challenge {
        public final String id;
        public final long expiresAt;
        public final boolean enrollmentRequired;
        private Challenge(String id, long expiresAt, boolean enrollment) { this.id = id; this.expiresAt = expiresAt; enrollmentRequired = enrollment; }
        public boolean active() { return expiresAt > System.currentTimeMillis(); }
        public static Challenge parse(JsonObject response) throws IOException {
            String id = AdminSession.string(response, "challengeId"); long expiry = timestamp(response, "expiresAt");
            if (!trueField(response, "ok") || !trueField(response, "mfaRequired") || !tokenFree(response)
                    || !id.matches("mfa_[A-Za-z0-9_-]{43}") || expiry <= System.currentTimeMillis()) throw new IOException("Invalid MFA challenge");
            if (response.has("enrollmentRequired") && (!response.get("enrollmentRequired").isJsonPrimitive()
                    || !response.getAsJsonPrimitive("enrollmentRequired").isBoolean())) throw new IOException("Invalid MFA challenge");
            return new Challenge(id, expiry, trueField(response, "enrollmentRequired"));
        }
    }
    public static final class Enrollment {
        public final String id, secret;
        public final long expiresAt;
        private Enrollment(String id, String secret, long expiry) { this.id = id; this.secret = secret; expiresAt = expiry; }
        public boolean active() { return expiresAt > System.currentTimeMillis(); }
        public static Enrollment parse(JsonObject response) throws IOException {
            String id = AdminSession.string(response, "enrollmentId"), seed = AdminSession.string(response, "secret");
            long expiry = timestamp(response, "expiresAt");
            if (!trueField(response, "ok") || !tokenFree(response) || !id.matches("enroll_[A-Za-z0-9_-]{43}")
                    || !seed.matches("[A-Z2-7]{32}") || expiry <= System.currentTimeMillis()) throw new IOException("Invalid MFA enrollment");
            return new Enrollment(id, seed, expiry);
        }
    }
    public static List<String> recoveryCodes(JsonObject response) throws IOException {
        try {
            if (!trueField(response, "ok") || !tokenFree(response) || !response.get("recoveryCodes").isJsonArray()
                    || response.getAsJsonArray("recoveryCodes").size() != 10) throw new IOException("Invalid recovery codes");
            List<String> codes = new ArrayList<>();
            for (JsonElement value : response.getAsJsonArray("recoveryCodes")) {
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
                        || !value.getAsString().matches("[A-F0-9]{32}")) throw new IOException("Invalid recovery codes");
                codes.add(value.getAsString());
            }
            if (new HashSet<>(codes).size() != 10) throw new IOException("Invalid recovery codes");
            return Collections.unmodifiableList(codes);
        } catch (RuntimeException invalid) { throw new IOException("Invalid recovery codes"); }
    }
    public static final class Required extends IOException {
        public final Challenge challenge;
        Required(Challenge challenge) { super("MFA required"); this.challenge = challenge; }
    }
}
