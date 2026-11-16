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
    public final boolean ownerPhoneTest;
    public final long mfaExpiresAt;
    private AdminAccess(String id, String role, Set<String> permissions, boolean verified, long expiry,boolean phoneTest) {
        accountId = id; this.role = role; this.permissions = Collections.unmodifiableSet(permissions); mfaVerified = verified; mfaExpiresAt = expiry;
        ownerPhoneTest=phoneTest;
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
            String scope=AdminSession.string(admin,"testScope");
            if(admin.has("testScope")&&!"owner_phone".equals(scope))return null;
            return new AdminAccess(id, role, permissions, verified, OwnerMfa.timestamp(mfa, "expiresAt"),"owner_phone".equals(scope));
        } catch (RuntimeException invalid) { return null; }
    }
    public boolean isOwner() { return "owner".equals(role); }
    public boolean hasVerifiedMfa() { return mfaVerified && mfaExpiresAt > System.currentTimeMillis(); }
    // Server evidence, not the legacy admin role or client flag, authorizes OWNER UI.
    public boolean canReadRegistrations() {
        return ("dispatcher".equals(role) && permissions.contains("drivers.read"))
                || (isOwner() && hasVerifiedMfa() && permissions.contains("*"));
    }
    public boolean can(String permission) {
        return isOwner()?hasVerifiedMfa()&&permissions.contains("*"):"dispatcher".equals(role)&&permissions.contains(permission);
    }
    public boolean canReadOrders(){return !ownerPhoneTest&&can("orders.read");}
    public boolean canAssign(boolean reassign){return canReadOrders()&&can("drivers.read")&&can("vehicles.read")&&can(reassign?"orders.reassign":"orders.assign");}
    public boolean canReadReview(){return can("drivers.verify_documents")||can("drivers.verify_identity")||can("vehicles.verify");}
    public boolean canStartReview(){return can("drivers.verify_documents")||can("drivers.verify_identity");}
    public boolean canFinalizeReview(){return can("drivers.verify_documents")&&can("drivers.verify_identity")&&can("vehicles.verify");}
    public boolean canReviewItem(RegistrationReview.Item item){return can(item.isVehicle()||"vehicle_photo".equals(item.type)?"vehicles.verify":"drivers.verify_documents");}
    public boolean canReadResource(RegistrationReview.Item item){
        if(ownerPhoneTest)return false;
        if("vehicle_photo".equals(item.type))return can("vehicles.verify");
        boolean identity="passport_selfie".equals(item.kind)||"identity_reference_selfie".equals(item.kind);
        return "document".equals(item.type)&&can("documents.read_sensitive")&&can(identity?"drivers.verify_identity":"drivers.verify_documents");
    }
}
