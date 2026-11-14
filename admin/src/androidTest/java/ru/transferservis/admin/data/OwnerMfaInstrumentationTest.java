package ru.transferservis.admin.data;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class OwnerMfaInstrumentationTest {
    private Context context;
    private AdminSessionStore store;
    @Before public void setup(){context=InstrumentationRegistry.getInstrumentation().getTargetContext();store=new AdminSessionStore(context);store.clear();}
    @After public void cleanup(){store.clear();}
    private String token(String prefix){StringBuilder s=new StringBuilder(prefix+"_");for(int i=0;i<43;i++)s.append('a');return s.toString();}
    private JsonObject challengeBody(boolean enrollment){JsonObject b=new JsonObject();b.addProperty("ok",true);b.addProperty("mfaRequired",true);b.addProperty("challengeId",token("mfa"));b.addProperty("expiresAt",System.currentTimeMillis()+60000);if(enrollment)b.addProperty("enrollmentRequired",true);return b;}
    private AdminSession session(String id){long now=System.currentTimeMillis();return new AdminSession(id,"Fixture","access-fixture",now+60000,"refresh-fixture",now+120000);}
    private JsonObject profile(boolean verified){
        JsonObject b=new JsonObject();b.addProperty("ok",true);JsonObject a=new JsonObject();b.add("admin",a);a.addProperty("accountId","owner-1");a.addProperty("role","owner");JsonArray permissions=new JsonArray();permissions.add("*");a.add("permissions",permissions);
        if(verified){JsonObject m=new JsonObject();m.addProperty("required",true);m.addProperty("verified",true);m.addProperty("method","totp");m.addProperty("expiresAt",System.currentTimeMillis()+60000);a.add("mfa",m);}return b;
    }
    private Response response(Request request,JsonObject body){return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("fixture").body(ResponseBody.create(MediaType.get("application/json"),body.toString())).build();}
    @Test public void passwordChallengeNeverPersistsSessionAndClearsPreviousAccount() throws Exception {
        assertTrue(store.save(session("previous-dispatcher")));AtomicInteger revoked=new AtomicInteger();
        AdminRepository repo=new AdminRepository(context,new AdminApiClient(chain->{
            if(chain.request().url().encodedPath().equals("/staff/logout")){revoked.incrementAndGet();JsonObject ok=new JsonObject();ok.addProperty("ok",true);return response(chain.request(),ok);}
            return response(chain.request(),challengeBody(false));
        }));
        try{try{repo.login("fixture","fixture-password",repo.generation());fail();}catch(OwnerMfa.Required expected){assertTrue(expected.challenge.active());assertNull(store.load());assertEquals(1,revoked.get());}}
        finally{repo.executor.shutdownNow();}
    }
    @Test public void verifiedOwnerSessionIsStoredOnlyAfterAuthoritativeMfaProfile() throws Exception {
        AdminRepository repo=new AdminRepository(context,new AdminApiClient(chain->response(chain.request(),chain.request().url().encodedPath().equals("/v1/admin/me")?profile(true):session("owner-1").toResponse())));
        try{
            AdminAccess access=repo.verifyMfa(OwnerMfa.Challenge.parse(challengeBody(false)),"123456",false,repo.generation());
            assertTrue(access.hasVerifiedMfa());assertTrue(access.canReadRegistrations());assertEquals("owner-1",store.load().accountId);
            JsonObject stored=store.load().toResponse();for(String key:new String[]{"secret","code","recoveryCode","recoveryCodes","challengeId","bootstrapCode"})assertFalse(stored.has(key));
        }finally{repo.executor.shutdownNow();}
    }
    @Test public void missingMfaProfileAfterVerifyCannotCreateOwnerSession() throws Exception {
        AdminRepository repo=new AdminRepository(context,new AdminApiClient(chain->response(chain.request(),chain.request().url().encodedPath().equals("/v1/admin/me")?profile(false):session("owner-1").toResponse())));
        try{try{repo.verifyMfa(OwnerMfa.Challenge.parse(challengeBody(false)),"123456",false,repo.generation());fail();}catch(java.io.IOException expected){assertNull(store.load());}}
        finally{repo.executor.shutdownNow();}
    }
    @Test public void cancelledMfaCannotRestoreSessionOrReadOwnerProfile() throws Exception {
        AtomicReference<AdminRepository> reference=new AtomicReference<>();AtomicInteger profiles=new AtomicInteger();
        AdminRepository repo=new AdminRepository(context,new AdminApiClient(chain->{
            String path=chain.request().url().encodedPath();
            if(path.equals("/v1/admin/mfa/totp/verify")){reference.get().logout();return response(chain.request(),session("owner-1").toResponse());}
            if(path.equals("/v1/admin/me"))profiles.incrementAndGet();JsonObject ok=new JsonObject();ok.addProperty("ok",true);return response(chain.request(),ok);
        }));reference.set(repo);
        try{long epoch=repo.generation();try{repo.verifyMfa(OwnerMfa.Challenge.parse(challengeBody(false)),"123456",false,epoch);fail();}catch(java.io.IOException expected){assertNull(store.load());assertEquals(0,profiles.get());}}
        finally{repo.executor.shutdownNow();}
    }
    @Test public void enrollmentSeedAndRecoveryCodesNeverEnterPersistentStorage() throws Exception {
        AdminRepository repo=new AdminRepository(context,new AdminApiClient(chain->{
            JsonObject b=new JsonObject();b.addProperty("ok",true);
            if(chain.request().url().encodedPath().endsWith("/start")){b.addProperty("enrollmentId",token("enroll"));b.addProperty("secret","GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");b.addProperty("expiresAt",System.currentTimeMillis()+60000);}
            else{JsonArray codes=new JsonArray();for(int i=0;i<10;i++)codes.add(String.format(java.util.Locale.ROOT,"%032X",i));b.add("recoveryCodes",codes);}return response(chain.request(),b);
        }));
        try{
            OwnerMfa.Enrollment enrollment=repo.startEnrollment(OwnerMfa.Challenge.parse(challengeBody(true)),"fixture-bootstrap");
            assertEquals(32,enrollment.secret.length());assertEquals(10,repo.confirmEnrollment(enrollment,"123456").size());assertNull(store.load());
            assertTrue(context.getSharedPreferences("admin_secure_session_v1",Context.MODE_PRIVATE).getAll().isEmpty());
        }finally{repo.executor.shutdownNow();}
    }
}
