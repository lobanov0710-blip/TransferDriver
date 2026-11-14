package ru.transferservis.admin.data;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;
public final class AdminSessionTest {
    private AdminSession session() { return new AdminSession("admin-1","Admin","access-test",System.currentTimeMillis()+60000,"refresh-test",System.currentTimeMillis()+120000); }
    @Test public void adminRoundTripAndTokenLifetimes() {
        AdminSession s = session(), parsed = AdminSession.fromResponse(s.toResponse());
        assertNotNull(parsed); assertTrue(parsed.same(s)); assertTrue(parsed.accessUsable()); assertTrue(parsed.refreshUsable());
        assertFalse(new AdminSession("a","A","a",1,"r",1).accessUsable());
        assertFalse(new AdminSession("a","A","a",1,"r",1).refreshUsable());
    }
    @Test public void driverOrOwnerAsLegacyAuthRoleRejected() {
        for (String role : new String[]{"driver","owner","dispatcher",""}) {
            JsonObject body=session().toResponse();body.getAsJsonObject("staff").addProperty("role",role);
            assertNull(AdminSession.fromResponse(body));
        }
    }
    @Test public void malformedTimestampsAndTokensRejected() {
        JsonObject body=session().toResponse();body.addProperty("accessExpiresAt",1.5);
        assertNull(AdminSession.fromResponse(body));
        body=session().toResponse();body.addProperty("accessExpiresAt","9999999999999");assertNull(AdminSession.fromResponse(body));
        body=session().toResponse();body.addProperty("accessToken","x\r\nAuthorization: bad");assertNull(AdminSession.fromResponse(body));
        body=session().toResponse();body.addProperty("ok","true");assertNull(AdminSession.fromResponse(body));
        assertFalse(new AdminSession(null,"A",null,1,null,1).valid());
    }
    @Test public void newAccountOrRefreshIsDifferentSession() {
        AdminSession s=session();
        assertFalse(s.same(new AdminSession("other","A",s.accessToken,s.accessExpiresAt,s.refreshToken,s.refreshExpiresAt)));
        assertFalse(s.same(new AdminSession(s.accountId,"A",s.accessToken,s.accessExpiresAt,"new-refresh",s.refreshExpiresAt)));
        assertFalse(s.same(null));
    }
}
