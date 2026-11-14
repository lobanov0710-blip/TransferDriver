package ru.transferservis.admin.data;

import com.google.gson.JsonObject;

public final class AdminSession {
    public final String accountId, displayName, accessToken, refreshToken;
    public final long accessExpiresAt, refreshExpiresAt;
    public AdminSession(String id, String name, String access, long accessUntil, String refresh, long refreshUntil) {
        accountId = id; displayName = name; accessToken = access; accessExpiresAt = accessUntil;
        refreshToken = refresh; refreshExpiresAt = refreshUntil;
    }
    public static String string(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || !obj.get(key).isJsonPrimitive()
                || !obj.getAsJsonPrimitive(key).isString()) return "";
        return obj.get(key).getAsString();
    }
    public static AdminSession fromResponse(JsonObject response) {
        try {
            if (response == null || !response.get("ok").isJsonPrimitive()
                    || !response.getAsJsonPrimitive("ok").isBoolean() || !response.get("ok").getAsBoolean()) return null;
            JsonObject staff = response.getAsJsonObject("staff");
            if (!"admin".equals(string(staff, "role"))) return null;
            AdminSession session = new AdminSession(string(staff, "id"), string(staff, "displayName"),
                    string(response, "accessToken"), timestamp(response, "accessExpiresAt"),
                    string(response, "refreshToken"), timestamp(response, "refreshExpiresAt"));
            return session.valid() ? session : null;
        } catch (RuntimeException invalid) { return null; }
    }
    private static long timestamp(JsonObject object, String name) {
        if (!object.get(name).isJsonPrimitive() || !object.getAsJsonPrimitive(name).isNumber()) return -1;
        return object.get(name).getAsBigDecimal().longValueExact();
    }
    public boolean valid() {
        return accountId != null && !accountId.trim().isEmpty() && accountId.equals(accountId.trim())
                && accessToken != null && accessToken.length() <= 8192 && accessToken.matches("[A-Za-z0-9._~-]+")
                && refreshToken != null && refreshToken.length() <= 1024 && refreshToken.matches("[A-Za-z0-9._~-]+")
                && accessExpiresAt > 0 && refreshExpiresAt > 0;
    }
    public boolean accessUsable() { return accessExpiresAt > System.currentTimeMillis() + 30_000; }
    public boolean refreshUsable() { return refreshExpiresAt > System.currentTimeMillis(); }
    public boolean same(AdminSession other) {
        return other != null && accountId.equals(other.accountId) && refreshToken.equals(other.refreshToken);
    }
    public JsonObject toResponse() {
        JsonObject staff = new JsonObject(); staff.addProperty("id", accountId);
        staff.addProperty("displayName", displayName); staff.addProperty("role", "admin");
        JsonObject data = new JsonObject(); data.addProperty("ok", true); data.add("staff", staff);
        data.addProperty("accessToken", accessToken); data.addProperty("accessExpiresAt", accessExpiresAt);
        data.addProperty("refreshToken", refreshToken); data.addProperty("refreshExpiresAt", refreshExpiresAt);
        return data;
    }
}
