package ru.transferservis.admin.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.util.Base64;
import android.view.WindowManager;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.transferservis.admin.R;
import ru.transferservis.admin.security.AdminEndpointPolicy;
import ru.transferservis.admin.ui.AdminActivity;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class AdminIsolationInstrumentationTest {
    private Context context;
    private AdminSessionStore store;
    private AdminSession session(String id,String suffix,boolean expired) {
        long now=System.currentTimeMillis();
        return new AdminSession(id,"Fixture Admin","access-"+suffix,expired?1:now+60000,"refresh-"+suffix,now+120000);
    }
    @Before public void setup() { context=InstrumentationRegistry.getInstrumentation().getTargetContext();store=new AdminSessionStore(context);store.clear(); }
    @After public void cleanup() { store.clear(); }
    @Test public void sessionRoundTripIsEncryptedAndLogoutInvalidatesKey() throws Exception {
        AdminSession original=session("admin-1","original",false);assertTrue(store.save(original));
        assertEquals(original.refreshToken,new AdminSessionStore(context).load().refreshToken);
        SharedPreferences prefs=context.getSharedPreferences("admin_secure_session_v1",Context.MODE_PRIVATE);
        String encrypted=prefs.getString("ciphertext","");assertFalse(encrypted.contains(original.refreshToken));
        KeyStore keys=KeyStore.getInstance("AndroidKeyStore");keys.load(null);assertTrue(keys.containsAlias("transfer_admin_session_key_v1"));
        store.clear();assertNull(store.load());assertTrue(prefs.getAll().isEmpty());assertFalse(keys.containsAlias("transfer_admin_session_key_v1"));
    }
    @Test public void delayedRotationAndFailureCannotOverwriteNewLoginOrRestoreLogout() {
        AdminSession original=session("admin-1","old",false),newLogin=session("admin-2","new",false);
        assertTrue(store.save(original));assertTrue(new AdminSessionStore(context).save(newLogin));
        assertNull(store.rotateIfCurrent(original,session("admin-1","rotated",false)));store.clearIfCurrent(original);
        assertEquals(newLogin.refreshToken,store.load().refreshToken);
        store.clear();assertNull(store.rotateIfCurrent(newLogin,session("admin-2","rotated",false)));assertNull(store.load());
    }
    @Test public void driverAndForeignBackendCiphertextRequireNewLogin() throws Exception {
        for(String aad:new String[]{"ru.transferservis.driver|driver-session|v2|"+AdminEndpointPolicy.BASE_URL,
                "ru.transferservis.admin|staff-session|v1|"+(AdminEndpointPolicy.BASE_URL.equals(AdminEndpointPolicy.STAGING)?AdminEndpointPolicy.PRODUCTION:AdminEndpointPolicy.STAGING)}) {
            AdminSession original=session("admin-1","original",false);assertTrue(store.save(original));
            KeyStore keys=KeyStore.getInstance("AndroidKeyStore");keys.load(null);
            SecretKey key=((KeyStore.SecretKeyEntry)keys.getEntry("transfer_admin_session_key_v1",null)).getSecretKey();
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key);cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext=cipher.doFinal(original.toResponse().toString().getBytes(StandardCharsets.UTF_8));
            SharedPreferences prefs=context.getSharedPreferences("admin_secure_session_v1",Context.MODE_PRIVATE);
            assertTrue(prefs.edit().putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP))
                    .putString("ciphertext",Base64.encodeToString(ciphertext,Base64.NO_WRAP)).commit());
            assertNull(store.load());assertTrue(prefs.getAll().isEmpty());
        }
    }
    @Test public void driverPreferenceNamesAreIgnoredAndCorruptAdminBlobCleared() {
        SharedPreferences driver=context.getSharedPreferences("driver_secure_session_v1",Context.MODE_PRIVATE);
        driver.edit().putString("session_iv","fixture").putString("session_ciphertext","fixture").commit();
        try {assertNull(store.load());}finally{driver.edit().clear().commit();}
        SharedPreferences admin=context.getSharedPreferences("admin_secure_session_v1",Context.MODE_PRIVATE);
        admin.edit().putInt("iv",42).putString("ciphertext","bad").commit();assertNull(store.load());assertTrue(admin.getAll().isEmpty());
    }
    @Test public void ownerProfileDoesNotOpenOperationalQueueWithoutMfa() throws Exception {
        AtomicInteger queueRequests=new AtomicInteger();AdminSession owner=session("admin-1","owner",false);
        AdminApiClient api=new AdminApiClient(chain->{
            String path=chain.request().url().encodedPath();String body;
            if(path.equals("/staff/login"))body=owner.toResponse().toString();
            else if(path.equals("/v1/admin/me"))body="{\"ok\":true,\"admin\":{\"accountId\":\"admin-1\",\"role\":\"owner\",\"permissions\":[\"*\"]}}";
            else {queueRequests.incrementAndGet();body="{\"ok\":true}";}
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("fixture")
                    .body(ResponseBody.create(MediaType.get("application/json"),body)).build();
        });
        AdminRepository repo=new AdminRepository(context,api);
        try {
            assertTrue(repo.login("fixture","fixture-password",repo.generation()).isOwner());
            try {repo.registrations();fail();}catch(java.io.IOException expected){assertEquals(0,queueRequests.get());}
        }finally{repo.executor.shutdownNow();}
    }
    @Test public void staleRefreshCannotBecomeAnotherAdminRequest() throws Exception {
        AdminSession original=session("admin-1","old",true),next=session("admin-2","new",false);assertTrue(store.save(original));
        AtomicInteger protectedReads=new AtomicInteger();
        AdminApiClient api=new AdminApiClient(chain->{
            if(!chain.request().url().encodedPath().equals("/staff/refresh")){protectedReads.incrementAndGet();throw new java.io.IOException("Unexpected protected read");}
            assertTrue(new AdminSessionStore(context).save(next));
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("fixture")
                    .body(ResponseBody.create(MediaType.get("application/json"),session("admin-1","rotated",false).toResponse().toString())).build();
        });
        AdminRepository repo=new AdminRepository(context,api);
        try {
            try {repo.me();fail();}catch(java.io.IOException expected){assertEquals(0,protectedReads.get());}
            assertEquals(next.refreshToken,store.load().refreshToken);
        }finally{repo.executor.shutdownNow();}
    }
    @Test public void temporaryRefreshFailurePreservesCurrentSession() throws Exception {
        AdminSession original=session("admin-1","old",true);assertTrue(store.save(original));
        AdminRepository repo=new AdminRepository(context,new AdminApiClient(chain->{throw new java.io.IOException("fixture network failure");}));
        try {try{repo.me();fail();}catch(java.io.IOException expected){assertEquals(original.refreshToken,store.load().refreshToken);}}
        finally{repo.executor.shutdownNow();}
    }
    @Test public void dispatcherReadsQueueWithRotatedSessionAndLogoutRevokesIt() throws Exception {
        AtomicInteger refreshes=new AtomicInteger(),queues=new AtomicInteger(),logouts=new AtomicInteger();
        AdminApiClient api=new AdminApiClient(chain->{
            String path=chain.request().url().encodedPath(),body;
            if(path.equals("/staff/login"))body=session("admin-1","old",false).toResponse().toString();
            else if(path.equals("/staff/refresh")){refreshes.incrementAndGet();body=session("admin-1","rotated",false).toResponse().toString();}
            else if(path.equals("/staff/logout")){logouts.incrementAndGet();body="{\"ok\":true}";}
            else if(path.equals("/v1/admin/me"))body="{\"ok\":true,\"admin\":{\"accountId\":\"admin-1\",\"role\":\"dispatcher\",\"permissions\":[\"drivers.read\"]}}";
            else {queues.incrementAndGet();assertEquals("Bearer access-rotated",chain.request().header("Authorization"));body="{\"ok\":true,\"registrations\":[{\"driverId\":\"fixture-driver\",\"fullName\":\"Fixture Driver\",\"registrationStatus\":\"submitted\"}]}";}
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("fixture")
                    .body(ResponseBody.create(MediaType.get("application/json"),body)).build();
        });
        AdminRepository repo=new AdminRepository(context,api);
        try {
            assertTrue(repo.login("fixture","fixture-password",repo.generation()).canReadRegistrations());
            assertTrue(store.save(session("admin-1","old",true)));
            AdminRepository.QueueResult result=repo.registrations();
            assertTrue(result.access.canReadRegistrations());assertEquals(1,RegistrationRow.parse(result.response).size());
            assertEquals(1,refreshes.get());assertEquals(1,queues.get());assertEquals("refresh-rotated",store.load().refreshToken);
            repo.logout();assertNull(store.load());repo.executor.submit(()->{}).get(5,java.util.concurrent.TimeUnit.SECONDS);assertEquals(1,logouts.get());
        }finally{repo.executor.shutdownNow();}
    }
    @Test public void revokedDispatcherPermissionStopsQueueBeforeTransport() throws Exception {
        assertTrue(store.save(session("admin-1","current",false)));AtomicInteger queues=new AtomicInteger();
        AdminRepository repo=new AdminRepository(context,new AdminApiClient(chain->{
            String path=chain.request().url().encodedPath();if(!path.equals("/v1/admin/me"))queues.incrementAndGet();
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("fixture")
                    .body(ResponseBody.create(MediaType.get("application/json"),"{\"ok\":true,\"admin\":{\"accountId\":\"admin-1\",\"role\":\"dispatcher\",\"permissions\":[]}}")).build();
        }));
        try{try{repo.registrations();fail();}catch(java.io.IOException expected){assertEquals(0,queues.get());assertNotNull(store.load());}}
        finally{repo.executor.shutdownNow();}
    }
    @Test public void wrongLegacyRoleIsNotStoredAndIssuedSessionIsRevoked() throws Exception {
        AtomicInteger logouts=new AtomicInteger();
        AdminRepository repo=new AdminRepository(context,new AdminApiClient(chain->{
            JsonObject data=session("driver-1","rejected",false).toResponse();data.getAsJsonObject("staff").addProperty("role","driver");
            if(chain.request().url().encodedPath().equals("/staff/logout")){logouts.incrementAndGet();data=new JsonObject();data.addProperty("ok",true);}
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("fixture")
                    .body(ResponseBody.create(MediaType.get("application/json"),data.toString())).build();
        }));
        try{try{repo.login("fixture","fixture-password",repo.generation());fail();}catch(java.io.IOException expected){assertNull(store.load());assertEquals(1,logouts.get());}}
        finally{repo.executor.shutdownNow();}
    }
    @Test public void delayedUnauthorizedDoesNotRetryAsNewAdmin() throws Exception {
        AdminSession old=session("admin-1","old",false),next=session("admin-2","new",false);assertTrue(store.save(old));
        AtomicInteger calls=new AtomicInteger();
        AdminRepository repo=new AdminRepository(context,new AdminApiClient(chain->{
            calls.incrementAndGet();assertEquals("/v1/admin/me",chain.request().url().encodedPath());
            assertEquals("Bearer access-old",chain.request().header("Authorization"));assertTrue(new AdminSessionStore(context).save(next));
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(401).message("fixture")
                    .body(ResponseBody.create(MediaType.get("application/json"),"{}")).build();
        }));
        try{try{repo.me();fail();}catch(java.io.IOException expected){assertEquals(1,calls.get());assertEquals(next.refreshToken,store.load().refreshToken);}}
        finally{repo.executor.shutdownNow();}
    }
    @Test public void secureScreenDoesNotSaveCredentials() {
        try(ActivityScenario<AdminActivity> activity=ActivityScenario.launch(AdminActivity.class)) {
            activity.onActivity(screen->{
                assertTrue((screen.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);
                assertFalse(((EditText)screen.findViewById(R.id.password)).isSaveEnabled());
                assertFalse(((EditText)screen.findViewById(R.id.login)).isSaveEnabled());
            });
        }
    }
    @Test public void installedAdminAndDriverHaveDifferentUids() throws Exception {
        assertEquals("ru.transferservis.admin",context.getPackageName());
        // Public package metadata only; no app data, credentials or security settings are read/changed.
        try(ParcelFileDescriptor.AutoCloseInputStream input=new ParcelFileDescriptor.AutoCloseInputStream(
                InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand("cmd package list packages -U"))) {
            java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[4096];int read;
            while((read=input.read(buffer))!=-1)output.write(buffer,0,read);
            String list=output.toString("UTF-8"),adminUid=null,driverUid=null;
            for(String line:list.split("\n")) {
                if(line.startsWith("package:ru.transferservis.admin uid:"))adminUid=line.substring(line.indexOf("uid:")+4).trim();
                if(line.startsWith("package:ru.transferservis.driver uid:"))driverUid=line.substring(line.indexOf("uid:")+4).trim();
            }
            assertNotNull("Install Driver before Admin runtime tests",driverUid);assertNotNull(adminUid);assertNotEquals(driverUid,adminUid);
        }
    }
}
