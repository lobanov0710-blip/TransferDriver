package ru.transferservis.admin.data;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;
public final class OwnerMfaTest {
    private JsonObject challenge() {
        JsonObject body=new JsonObject();body.addProperty("ok",true);body.addProperty("mfaRequired",true);
        body.addProperty("challengeId","mfa_"+repeat('a',43));body.addProperty("expiresAt",System.currentTimeMillis()+60000);return body;
    }
    private String repeat(char c,int n){StringBuilder s=new StringBuilder();while(n-->0)s.append(c);return s.toString();}
    @Test public void challengeIsVolatileAndNeverAStaffSession() throws Exception {
        JsonObject body=challenge();assertTrue(OwnerMfa.Challenge.parse(body).active());assertNull(AdminSession.fromResponse(body));
        body.addProperty("enrollmentRequired",true);assertTrue(OwnerMfa.Challenge.parse(body).enrollmentRequired);
    }
    @Test public void mixedTokenExpiredAndMalformedChallengesFailClosed() {
        for(int kind=0;kind<5;kind++){
            JsonObject body=challenge();
            if(kind==0)body.addProperty("accessToken","fixture");if(kind==1)body.addProperty("refreshToken","fixture");
            if(kind==2)body.addProperty("expiresAt",1);if(kind==3)body.addProperty("challengeId","client-role-owner");if(kind==4)body.addProperty("mfaRequired","true");
            try{OwnerMfa.Challenge.parse(body);fail();}catch(java.io.IOException expected){assertTrue(expected.getMessage().length()<80);}
        }
    }
    @Test public void enrollmentRequiresBase32SeedAndNoWorkingTokens() throws Exception {
        JsonObject body=new JsonObject();body.addProperty("ok",true);body.addProperty("enrollmentId","enroll_"+repeat('a',43));
        body.addProperty("secret","GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");body.addProperty("expiresAt",System.currentTimeMillis()+60000);
        assertTrue(OwnerMfa.Enrollment.parse(body).active());assertNull(AdminSession.fromResponse(body));
        body.addProperty("refreshToken","fixture");try{OwnerMfa.Enrollment.parse(body);fail();}catch(java.io.IOException expected){assertFalse(expected.getMessage().contains(body.get("secret").getAsString()));}
    }
    @Test public void recoveryCodesMustBeTenUnique128BitValues() throws Exception {
        JsonObject body=new JsonObject();body.addProperty("ok",true);JsonArray codes=new JsonArray();for(int i=0;i<10;i++)codes.add(String.format(java.util.Locale.ROOT,"%032X",i));body.add("recoveryCodes",codes);
        assertEquals(10,OwnerMfa.recoveryCodes(body).size());codes.set(9,codes.get(0));
        try{OwnerMfa.recoveryCodes(body);fail();}catch(java.io.IOException expected){assertTrue(expected.getMessage().length()<80);}
    }
    @Test public void ownerGrantRequiresCurrentTypedServerEvidence() {
        JsonObject body=new JsonObject();body.addProperty("ok",true);JsonObject admin=new JsonObject();body.add("admin",admin);
        admin.addProperty("accountId","owner");admin.addProperty("role","owner");JsonArray permissions=new JsonArray();permissions.add("*");admin.add("permissions",permissions);
        assertFalse(AdminAccess.fromResponse(body,"owner").canReadRegistrations());JsonObject mfa=new JsonObject();admin.add("mfa",mfa);
        mfa.addProperty("required",true);mfa.addProperty("verified",true);mfa.addProperty("method","totp");mfa.addProperty("expiresAt",System.currentTimeMillis()+60000);
        assertTrue(AdminAccess.fromResponse(body,"owner").canReadRegistrations());mfa.addProperty("expiresAt",1);assertFalse(AdminAccess.fromResponse(body,"owner").canReadRegistrations());
        mfa.addProperty("expiresAt",System.currentTimeMillis()+60000);mfa.addProperty("verified","true");assertFalse(AdminAccess.fromResponse(body,"owner").canReadRegistrations());
    }
}
