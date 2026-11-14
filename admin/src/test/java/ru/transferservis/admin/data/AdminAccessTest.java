package ru.transferservis.admin.data;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import static org.junit.Assert.*;
public final class AdminAccessTest {
    private JsonObject profile(String role, String permissions) {
        return JsonParser.parseString("{\"ok\":true,\"admin\":{\"accountId\":\"admin-1\",\"role\":\""+role+"\",\"permissions\":"+permissions+"}}").getAsJsonObject();
    }
    @Test public void dispatcherNeedsExactExplicitPermission() {
        assertTrue(AdminAccess.fromResponse(profile("dispatcher", "[\"drivers.read\"]"),"admin-1").canReadRegistrations());
        for (String p : new String[]{"[]", "[\"*\"]", "[\"drivers.verify_documents\"]"})
            assertFalse(AdminAccess.fromResponse(profile("dispatcher",p),"admin-1").canReadRegistrations());
    }
    @Test public void ownerOperationsStayClosedUntilMfa() {
        AdminAccess access = AdminAccess.fromResponse(profile("owner","[\"*\"]"),"admin-1");
        assertTrue(access.isOwner()); assertFalse(access.canReadRegistrations());
    }
    @Test public void legacyAdminAndUnknownRoleNeverBecomeOwner() {
        for (String role : new String[]{"admin", "driver", "superadmin", "OWNER", ""})
            assertNull(AdminAccess.fromResponse(profile(role,"[\"*\"]"),"admin-1"));
    }
    @Test public void accountMustMatchAuthenticatedSession() {
        assertNull(AdminAccess.fromResponse(profile("owner","[\"*\"]"),"other-admin"));
    }
    @Test public void missingMalformedOrStringOkIsRejected() {
        for (String json : new String[]{"{}", "{\"ok\":true}", "{\"ok\":\"true\",\"admin\":{}}", "null"}) {
            try { assertNull(AdminAccess.fromResponse(JsonParser.parseString(json).isJsonObject()
                    ? JsonParser.parseString(json).getAsJsonObject() : null,"admin-1")); }
            catch (RuntimeException invalid) { fail("Parser must fail closed"); }
        }
        JsonObject invalid = profile("dispatcher","[1]"); assertNull(AdminAccess.fromResponse(invalid,"admin-1"));
    }
}
