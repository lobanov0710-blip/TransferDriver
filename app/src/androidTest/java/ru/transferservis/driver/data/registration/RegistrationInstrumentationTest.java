package ru.transferservis.driver.data.registration;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.transferservis.driver.R;
import ru.transferservis.driver.data.session.DriverSession;
import ru.transferservis.driver.data.session.SecureSessionStore;
import ru.transferservis.driver.security.ApiEndpointPolicy;
import ru.transferservis.driver.ui.registration.RegistrationActivity;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class RegistrationInstrumentationTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private final Context context=instrumentation.getTargetContext();
    private SecureSessionStore store;
    private RegistrationActivity activity;
    private long scope;
    private static final String VEHICLE="vehicle_00000000-0000-4000-8000-000000000001";
    private static final String DRAFT="{\"ok\":true,\"profile\":{\"driverId\":\"synthetic-driver\",\"phone\":\"+70000000000\",\"fullName\":null,\"birthDate\":null,\"region\":null,\"registrationStatus\":\"draft\",\"revision\":1},\"vehicles\":[]}";
    @Before public void before() {store=new SecureSessionStore(context);store.clear();}
    @After public void after() throws Exception {
        if(activity!=null)instrumentation.runOnMainSync(()->activity.finish());
        store.clear(); instrumentation.runOnMainSync(()->AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));
        shell("settings put system font_scale 1.0");
    }
    private DriverSession session(String id,String suffix) {
        long expiry=System.currentTimeMillis()+3_600_000L;
        return new DriverSession(id,"Synthetic Driver","driver","synthetic-access-"+suffix,expiry,"synthetic-refresh-"+suffix,expiry);
    }
    private Response response(Interceptor.Chain chain,int code,String json) {
        return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("Synthetic")
            .body(ResponseBody.create(MediaType.get("application/json"),json)).build();
    }
    private RegistrationRepository repository(Interceptor transport) throws Exception {
        assertTrue(store.save(session("synthetic-driver","original")));
        RegistrationRepository repository=new RegistrationRepository(context,transport);scope=repository.bind();return repository;
    }
    @Test public void profileAndVehicleMutationsHaveNoAutomatic401RefreshOrReplay() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        RegistrationRepository repository=repository(chain->{calls.incrementAndGet();assertEquals("Bearer synthetic-access-original",chain.request().header("Authorization"));
            assertTrue(ApiEndpointPolicy.isAllowed(chain.request().url().uri()));assertEquals("no-store",chain.request().header("Cache-Control"));
            return response(chain,401,"{\"code\":\"authorization_required\"}");});
        for(int operation=0;operation<3;operation++)try {
            if(operation==0)repository.saveProfile(scope,new JsonObject());else repository.saveVehicle(scope,operation==1?null:VEHICLE,new JsonObject());fail();
        }catch(RegistrationRepository.Failure expected){assertEquals(401,expected.status);}
        assertEquals(3,calls.get());assertEquals("synthetic-refresh-original",store.load().getRefreshToken());
    }
    @Test public void ambiguousVehicleCreateIsSentExactlyOnce() throws Exception {
        AtomicInteger calls=new AtomicInteger();RegistrationRepository repository=repository(chain->{calls.incrementAndGet();throw new IOException("Synthetic disconnect");});
        try{repository.saveVehicle(scope,null,new JsonObject());fail();}catch(IOException expected){assertEquals(1,calls.get());}
    }
    @Test public void changedAccountStopsQueuedMutationBeforeNetwork() throws Exception {
        AtomicInteger calls=new AtomicInteger();RegistrationRepository repository=repository(chain->{calls.incrementAndGet();return response(chain,200,DRAFT);});
        assertTrue(store.save(session("another-driver","new")));
        try{repository.saveProfile(scope,new JsonObject());fail();}catch(RegistrationRepository.Failure expected){assertEquals(401,expected.status);}
        assertEquals(0,calls.get());assertEquals("another-driver",store.load().getStaffId());
    }
    @Test public void sameAccountReloginCannotReuseOldFormSession() throws Exception {
        RegistrationRepository repository=repository(chain->{fail("Old form reached network");return null;});
        assertTrue(store.save(session("synthetic-driver","relogin")));
        try{repository.saveVehicle(scope,VEHICLE,new JsonObject());fail();}catch(RegistrationRepository.Failure expected){assertEquals(401,expected.status);}
    }
    @Test public void delayedResponseCannotExposePreviousAccountData() throws Exception {
        RegistrationRepository repository=repository(chain->{assertTrue(store.save(session("another-driver","new")));return response(chain,200,DRAFT);});
        try{repository.load(scope);fail();}catch(RegistrationRepository.Failure expected){assertEquals(401,expected.status);}
        assertEquals("another-driver",store.load().getStaffId());
    }
    @Test public void oversizedReplyIsRejected() throws Exception {
        RegistrationRepository repository=repository(chain->response(chain,200," ".repeat(1_048_577)));
        try{repository.load(scope);fail();}catch(IOException expected){assertEquals("Invalid response",expected.getMessage());}
    }
    @Test public void redirectCannotForwardCredentialsAndRawErrorsStayPrivate() throws Exception {
        AtomicInteger calls=new AtomicInteger();RegistrationRepository repository=repository(chain->{calls.incrementAndGet();return response(chain,302,"{\"code\":\"bad\",\"error\":\"synthetic-provider-private-output\"}")
                .newBuilder().header("Location","https://example.invalid/private").build();});
        try{repository.load(scope);fail();}catch(RegistrationRepository.Failure expected){assertEquals(302,expected.status);assertFalse(expected.getMessage().contains("provider"));}
        assertEquals(1,calls.get());
    }
    @Test public void statusConflictIsReturnedWithoutRetry() throws Exception {
        AtomicInteger calls=new AtomicInteger();RegistrationRepository repository=repository(chain->{calls.incrementAndGet();return response(chain,409,"{\"code\":\"revision_conflict\"}");});
        try{repository.saveVehicle(scope,VEHICLE,new JsonObject());fail();}catch(RegistrationRepository.Failure expected){assertEquals("revision_conflict",expected.code);}
        assertEquals(1,calls.get());
    }
    @Test public void cancelledQueuedMutationCannotRunAfterRebind() throws Exception {
        AtomicInteger calls=new AtomicInteger();RegistrationRepository repository=repository(chain->{calls.incrementAndGet();return response(chain,200,DRAFT);});
        long oldScope=scope;repository.cancel();scope=repository.bind();
        try{repository.saveVehicle(oldScope,null,new JsonObject());fail();}catch(RegistrationRepository.Failure expected){assertEquals(401,expected.status);}
        assertEquals(0,calls.get());assertNotNull(repository.load(scope));assertEquals(1,calls.get());
    }
    private void launch(Interceptor transport) throws Exception {
        store.clear();
        activity=(RegistrationActivity)instrumentation.startActivitySync(new Intent(context,RegistrationActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        awaitText("Сессия изменилась"); // No usable session at launch: no real request can leave the app.
        RegistrationRepository repository=repository(transport);
        Field field=RegistrationActivity.class.getDeclaredField("repository");field.setAccessible(true);
        instrumentation.runOnMainSync(()->{try{((RegistrationRepository)field.get(activity)).cancel();field.set(activity,repository);activity.findViewById(R.id.registrationReload).performClick();}catch(IllegalAccessException failure){throw new AssertionError(failure);}});
        awaitText("Статус анкеты");
    }
    private void awaitText(String text) throws Exception {
        long deadline=System.currentTimeMillis()+8000;
        while(System.currentTimeMillis()<deadline){final boolean[] found={false};instrumentation.runOnMainSync(()->found[0]=((TextView)activity.findViewById(R.id.registrationMessage)).getText().toString().contains(text));if(found[0])return;Thread.sleep(80);}
        fail("Expected safe UI state was not reached: "+text);
    }
    private void click(int id){instrumentation.runOnMainSync(()->activity.findViewById(id).performClick());}
    private void enter(int id,String text){instrumentation.runOnMainSync(()->((EditText)activity.findViewById(id)).setText(text));}
    @Test public void uiSavesRevisionAndConfirmedPhoneCannotBeEdited() throws Exception {
        AtomicInteger writes=new AtomicInteger();JsonObject data=JsonParser.parseString(DRAFT).getAsJsonObject();
        launch(chain->{if(chain.request().method().equals("PUT")){
            writes.incrementAndGet();okio.Buffer buffer=new okio.Buffer();chain.request().body().writeTo(buffer);JsonObject body=JsonParser.parseString(buffer.readUtf8()).getAsJsonObject();
            assertEquals(4,body.size());assertEquals(1,body.get("expectedRevision").getAsInt());assertEquals("2000-02-29",body.get("birthDate").getAsString());assertFalse(body.has("phone"));
            data.getAsJsonObject("profile").addProperty("fullName",body.get("fullName").getAsString());data.getAsJsonObject("profile").addProperty("birthDate","2000-02-29");data.getAsJsonObject("profile").addProperty("region","Synthetic Region");data.getAsJsonObject("profile").addProperty("revision",2);
            return response(chain,200,"{\"ok\":true}");}return response(chain,200,data.toString());});
        enter(R.id.fullNameInput,"Synthetic Driver");enter(R.id.birthDateInput,"29.02.2000");enter(R.id.regionInput,"Synthetic Region");click(R.id.registrationSaveProfile);awaitText("Сохранено");assertEquals(1,writes.get());
        instrumentation.runOnMainSync(()->{assertFalse(activity.findViewById(R.id.registrationPhone) instanceof EditText);assertTrue((activity.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);});
        assertFalse(context.getPackageManager().getActivityInfo(activity.getComponentName(),0).exported);
    }
    @Test public void ambiguousUiMutationRequiresReloadBeforeAnotherSave() throws Exception {
        AtomicInteger writes=new AtomicInteger();launch(chain->{if(chain.request().method().equals("PUT")){writes.incrementAndGet();throw new IOException("Synthetic disconnect");}return response(chain,200,DRAFT);});
        enter(R.id.fullNameInput,"Synthetic Driver");enter(R.id.birthDateInput,"29.02.2000");enter(R.id.regionInput,"Synthetic Region");click(R.id.registrationSaveProfile);awaitText("могло сохраниться");
        assertEquals(1,writes.get());instrumentation.runOnMainSync(()->assertEquals(View.GONE,activity.findViewById(R.id.registrationOverview).getVisibility()));
        click(R.id.registrationSaveProfile);assertEquals(1,writes.get());click(R.id.registrationReload);awaitText("Статус анкеты");
    }
    @Test public void vehicleUiRequiresVinAndSavesSeparateVehicleRevision() throws Exception {
        AtomicInteger writes=new AtomicInteger();JsonObject data=JsonParser.parseString(DRAFT).getAsJsonObject();
        launch(chain->{String method=chain.request().method();if(!method.equals("GET")){
            writes.incrementAndGet();okio.Buffer buffer=new okio.Buffer();chain.request().body().writeTo(buffer);JsonObject body=JsonParser.parseString(buffer.readUtf8()).getAsJsonObject();
            assertFalse(body.has("driverId"));assertFalse(body.has("status"));assertEquals("XW8ZZZ61ZFG000001",body.get("vin").getAsString());
            if(method.equals("POST")){assertEquals(9,body.size());body.addProperty("id",VEHICLE);body.addProperty("driverId","synthetic-driver");body.addProperty("status","draft");body.addProperty("revision",1);data.getAsJsonArray("vehicles").add(body);}
            else {assertEquals("PATCH",method);assertEquals(10,body.size());assertEquals(1,body.get("expectedRevision").getAsInt());data.getAsJsonArray("vehicles").get(0).getAsJsonObject().addProperty("revision",2);}
            return response(chain,200,"{\"ok\":true}");}return response(chain,200,data.toString());});
        click(R.id.registrationAddVehicle);click(R.id.registrationSaveVehicle);awaitText("Проверьте поля");assertEquals(0,writes.get());
        enter(R.id.vinInput,"xw8zzz61zfg000001");enter(R.id.plateInput,"TEST-001");enter(R.id.makeInput,"Synthetic");enter(R.id.modelInput,"Vehicle");enter(R.id.capacityInput,"4");
        instrumentation.runOnMainSync(()->{for(int id:new int[]{R.id.classInput,R.id.baggageInput,R.id.usageInput})((android.widget.Spinner)activity.findViewById(id)).setSelection(1);});
        click(R.id.registrationSaveVehicle);awaitText("Сохранено");assertEquals(1,writes.get());
        instrumentation.runOnMainSync(()->((android.widget.LinearLayout)activity.findViewById(R.id.registrationVehicles)).getChildAt(0).performClick());
        click(R.id.registrationSaveVehicle);awaitText("Сохранено");assertEquals(2,writes.get());
    }
    @Test public void backgroundClearsUnsavedInputAndSecureCaptureFlagRemains() throws Exception {
        launch(chain->response(chain,200,DRAFT));enter(R.id.fullNameInput,"UNSAVED_SYNTHETIC_NAME");
        android.os.Bundle state=new android.os.Bundle();
        instrumentation.runOnMainSync(()->instrumentation.callActivityOnSaveInstanceState(activity,state));
        android.os.Parcel parcel=android.os.Parcel.obtain();
        try{parcel.writeBundle(state);assertFalse(new String(parcel.marshall(),java.nio.charset.StandardCharsets.UTF_16LE).contains("UNSAVED_SYNTHETIC_NAME"));}finally{parcel.recycle();}
        instrumentation.runOnMainSync(()->instrumentation.callActivityOnStop(activity));
        instrumentation.runOnMainSync(()->{assertEquals("",((EditText)activity.findViewById(R.id.fullNameInput)).getText().toString());assertTrue((activity.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);});
    }
    @Test public void nativeSyntheticScreensLight() throws Exception {nativeScreens(false);}
    @Test public void nativeSyntheticScreensDarkLargeFont() throws Exception {nativeScreens(true);}
    private void nativeScreens(boolean dark) throws Exception {
        shell("settings put system font_scale "+(dark?"1.3":"1.0"));
        instrumentation.runOnMainSync(()->AppCompatDelegate.setDefaultNightMode(dark?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO));
        launch(chain->response(chain,200,DRAFT));
        instrumentation.runOnMainSync(()->{
            androidx.core.view.WindowInsetsControllerCompat bars=androidx.core.view.WindowCompat.getInsetsController(activity.getWindow(),activity.getWindow().getDecorView());
            assertEquals(!dark,bars.isAppearanceLightStatusBars());assertEquals(!dark,bars.isAppearanceLightNavigationBars());
            assertEquals(dark?1.3f:1.0f,activity.getResources().getConfiguration().fontScale,0.01f);
        });
        capture(dark?"profile-dark-large":"profile-light");
        click(R.id.registrationAddVehicle);awaitText("Новый автомобиль");
        instrumentation.runOnMainSync(()->{
            com.google.android.material.textfield.TextInputLayout capacity=activity.findViewById(R.id.capacityLayout);
            EditText field=activity.findViewById(R.id.capacityInput);
            assertEquals("От 1 до 20",capacity.getHelperText().toString());
            assertTrue(field.getPaint().measureText(capacity.getHint().toString())<=field.getWidth()-field.getPaddingLeft()-field.getPaddingRight());
        });
        capture(dark?"vehicle-dark-large":"vehicle-light");
    }
    private void capture(String name) throws Exception {
        // Test APK only, mock transport and synthetic account only. Production FLAG_SECURE is unchanged.
        instrumentation.runOnMainSync(()->{assertTrue((activity.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);});
        try{Thread.sleep(250);Bitmap screenshot=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(screenshot);
            File directory=new File(context.getFilesDir(),"registration-evidence");assertTrue(directory.isDirectory()||directory.mkdirs());
            try(FileOutputStream stream=new FileOutputStream(new File(directory,name+".png"))){assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG,100,stream));}finally{screenshot.recycle();}
        }finally{instrumentation.runOnMainSync(()->{activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);assertTrue((activity.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);});}
    }
    private void shell(String command) throws Exception {try(java.io.InputStream stream=new ParcelFileDescriptor.AutoCloseInputStream(instrumentation.getUiAutomation().executeShellCommand(command))){byte[] buffer=new byte[512];while(stream.read(buffer)!=-1){ }}}
}
