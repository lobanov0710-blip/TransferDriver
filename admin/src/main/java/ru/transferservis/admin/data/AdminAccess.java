package ru.transferservis.admin.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class AdminAccess {
    public final String accountId, role;
    private final Set<String> permissions;
    private final boolean mfaVerified;
    public final long mfaExpiresAt;
    private AdminAccess(String id, String role, Set<String> permissions, boolean verified, long expiry) {
        accountId = id; this.role = role; this.permissions = Collections.unmodifiableSet(permissions); mfaVerified = verified; mfaExpiresAt = expiry;
    }
    public static AdminAccess fromResponse(JsonObject response, String expectedAccount) {
        try {
            if (response == null || !response.get("ok").isJsonPrimitive()
                    || !response.getAsJsonPrimitive("ok").isBoolean() || !response.get("ok").getAsBoolean()) return null;
            JsonObject admin = response.getAsJsonObject("admin");
            String id = AdminSession.string(admin, "accountId"), role = AdminSession.string(admin, "role");
            if (id.isEmpty() || !id.equals(expectedAccount)
                    || !("owner".equals(role) || "dispatcher".equals(role))
                    || !admin.get("permissions").isJsonArray()) return null;
            Set<String> permissions = new HashSet<>();
            for (JsonElement item : admin.getAsJsonArray("permissions")) {
                if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) return null;
                permissions.add(item.getAsString());
            }
            JsonObject mfa = admin.has("mfa") && admin.get("mfa").isJsonObject() ? admin.getAsJsonObject("mfa") : null;
            String method = AdminSession.string(mfa, "method");
            boolean verified = OwnerMfa.trueField(mfa, "required") && OwnerMfa.trueField(mfa, "verified")
                    && ("totp".equals(method) || "recovery".equals(method));
            return new AdminAccess(id, role, permissions, verified, OwnerMfa.timestamp(mfa, "expiresAt"));
        } catch (RuntimeException invalid) { return null; }
    }
    public boolean isOwner() { return "owner".equals(role); }
    public boolean hasVerifiedMfa() { return mfaVerified && mfaExpiresAt > System.currentTimeMillis(); }
    // Server evidence, not the legacy admin role or client flag, authorizes OWNER UI.
    public boolean canReadRegistrations() {
        return ("dispatcher".equals(role) && permissions.contains("drivers.read"))
                || (isOwner() && hasVerifiedMfa() && permissions.contains("*"));
    }
}
