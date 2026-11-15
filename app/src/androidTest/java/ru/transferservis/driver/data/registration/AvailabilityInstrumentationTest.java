package ru.transferservis.driver.data.registration;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.ViewCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
import ru.transferservis.driver.ui.main.AvailabilityPanel;
import ru.transferservis.driver.ui.main.MainWindow;
import ru.transferservis.driver.ui.registration.RegistrationActivity;
import static org.junit.Assert.*;

/** Real native main XML/panel in a secure test host; no live Main login/order routing is claimed. */
@RunWith(AndroidJUnit4.class)
public final class AvailabilityInstrumentationTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private final Context context=instrumentation.getTargetContext();
    private SecureSessionStore store;
    private RegistrationActivity host;
    private View main;
    private AvailabilityPanel panel;
    private java.net.ProxySelector originalProxy;
    private static final String CAR1="vehicle_00000000-0000-4000-8000-000000000001",CAR2="vehicle_00000000-0000-4000-8000-000000000002";
    @Before public void before(){
        store=new SecureSessionStore(context);store.clear();originalProxy=java.net.ProxySelector.getDefault();
        java.net.ProxySelector.setDefault(new java.net.ProxySelector(){
            @Override public java.util.List<java.net.Proxy> select(java.net.URI ignored){throw new AssertionError("Unmocked registration network forbidden");}
            @Override public void connectFailed(java.net.URI ignored,java.net.SocketAddress address,IOException error){ }
        });
    }
    @After public void after() throws Exception {
        try{
            store.clear();if(host!=null){instrumentation.runOnMainSync(()->host.finish());long until=System.currentTimeMillis()+8000;
                while(!host.isDestroyed()&&System.currentTimeMillis()<until){instrumentation.waitForIdleSync();Thread.sleep(40);}assertTrue(host.isDestroyed());}
            instrumentation.runOnMainSync(()->AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));shell("settings put system font_scale 1.0");awaitFont(1.0f);
        }finally{java.net.ProxySelector.setDefault(originalProxy);}
    }
    private DriverSession session(String suffix){long expiry=System.currentTimeMillis()+3600000;return new DriverSession("synthetic-driver","Synthetic Driver","driver","access-"+suffix,expiry,"refresh-"+suffix,expiry);}
    private Response reply(Interceptor.Chain chain,int status,String json){return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(status).message("Synthetic").body(ResponseBody.create(MediaType.get("application/json"),json)).build();}
    private final class Server implements Interceptor {
        final AtomicInteger puts=new AtomicInteger();String state="off_line",vehicle=CAR1,registration="approved",identity="verified",failure="",lastKey="",lastVehicle="";
        long revision=7,lastRevision;int failureStatus=422;boolean disconnect,missingRevision;CountDownLatch entered,release;
        private JsonObject snapshot(){
            JsonObject r=JsonParser.parseString("{\"ok\":true,\"profile\":{\"driverId\":\"synthetic-driver\",\"phone\":\"+70000000000\",\"fullName\":null,\"birthDate\":null,\"region\":null,\"registrationStatus\":\"approved\",\"identityStatus\":\"verified\",\"revision\":3},\"vehicles\":[]}").getAsJsonObject();
            JsonObject p=r.getAsJsonObject("profile");p.addProperty("registrationStatus",registration);p.addProperty("identityStatus",identity);
            JsonObject a=new JsonObject();a.addProperty("state",state);a.addProperty("activeVehicleId",vehicle);if(missingRevision)a.add("revision",com.google.gson.JsonNull.INSTANCE);else a.addProperty("revision",revision);p.add("availability",a);
            for(int i=1;i<=3;i++){JsonObject v=JsonParser.parseString("{\"driverId\":\"synthetic-driver\",\"vin\":\"XW8ZZZ61ZFG000001\",\"make\":\"Synthetic\",\"model\":\"Car\",\"color\":null,\"vehicleClass\":\"comfort\",\"capacity\":4,\"baggageCapacity\":\"standard\",\"usageBasis\":\"owner\",\"revision\":1}").getAsJsonObject();v.addProperty("id",i==1?CAR1:i==2?CAR2:"vehicle_00000000-0000-4000-8000-000000000003");v.addProperty("plate","SYNTH-00"+i);v.addProperty("status",i==3?"suspended":"approved");r.getAsJsonArray("vehicles").add(v);}return r;
        }
        @Override public Response intercept(Chain chain) throws IOException {
            assertEquals("Bearer access-original",chain.request().header("Authorization"));
            if(chain.request().method().equals("GET")){assertEquals("/v1/driver/me",chain.request().url().encodedPath());return reply(chain,200,snapshot().toString());}
            assertEquals("PUT",chain.request().method());assertEquals("/v1/driver/availability",chain.request().url().encodedPath());
            assertTrue(chain.request().body().isOneShot());assertEquals("no-store",chain.request().header("Cache-Control"));
            puts.incrementAndGet();okio.Buffer buffer=new okio.Buffer();chain.request().body().writeTo(buffer);JsonObject body=JsonParser.parseString(buffer.readUtf8()).getAsJsonObject();
            assertEquals(3,body.size());assertFalse(body.has("driverId"));lastKey=chain.request().header("Idempotency-Key");assertTrue(lastKey.matches("availability-[0-9a-f-]{36}"));
            lastRevision=body.get("expectedRevision").getAsLong();lastVehicle=body.get("activeVehicleId").getAsString();
            if(entered!=null)entered.countDown();if(release!=null)try{assertTrue(release.await(8,TimeUnit.SECONDS));}catch(InterruptedException cancelled){Thread.currentThread().interrupt();throw new IOException("Cancelled synthetic request");}
            if(!failure.isEmpty())return reply(chain,failureStatus,"{\"ok\":false,\"code\":\""+failure+"\",\"error\":\"SYNTHETIC_PRIVATE_PROVIDER_TEXT\"}");
            state=body.get("requestedState").getAsString();vehicle=lastVehicle;revision++;
            if(disconnect)throw new IOException("Synthetic lost acknowledgement");
            return reply(chain,200,"{\"ok\":true,\"availability\":{\"state\":\""+state+"\"}}");
        }
    }
    private void launch(Server server) throws Exception {
        store.clear();host=(RegistrationActivity)instrumentation.startActivitySync(new Intent(context,RegistrationActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        await(()->((TextView)host.findViewById(R.id.registrationMessage)).getText().toString().contains("Сессия изменилась"));
        assertTrue(store.save(session("original")));Field field=RegistrationActivity.class.getDeclaredField("repository");field.setAccessible(true);
        RegistrationRepository hidden=new RegistrationRepository(context,server),availability=new RegistrationRepository(context,server);
        instrumentation.runOnMainSync(()->{
            try{((RegistrationRepository)field.get(host)).cancel();field.set(host,hidden);}catch(IllegalAccessException e){throw new AssertionError(e);}
            FrameLayout frame=host.findViewById(android.R.id.content);frame.getChildAt(0).setVisibility(View.GONE);
            main=host.getLayoutInflater().inflate(R.layout.activity_driver_main,frame,false);frame.addView(main,new FrameLayout.LayoutParams(-1,-1));MainWindow.configure(host,main);
            ((TextView)main.findViewById(R.id.textDriverStatus)).setText("Синтетическая проверка доступности");main.findViewById(R.id.progressSession).setVisibility(View.GONE);
            for(int id:new int[]{R.id.buttonRegistration,R.id.layoutDriverTools,R.id.buttonLogout,R.id.tabOrders,R.id.swipeRefreshOrders,R.id.textOrdersEmpty})main.findViewById(id).setVisibility(View.VISIBLE);
            ((TextView)main.findViewById(R.id.textOrdersEmpty)).setText(R.string.orders_empty_active);
            TabLayout tabs=main.findViewById(R.id.tabOrders);for(int label:new int[]{R.string.orders_tab_new,R.string.orders_tab_active,R.string.orders_tab_history})tabs.addTab(tabs.newTab().setText(label));
            panel=new AvailabilityPanel(host,main.findViewById(R.id.availabilityPanel),availability);panel.start();
        });
        await(()->main.findViewById(R.id.availabilityProgress).getVisibility()==View.GONE);
    }
    private interface Condition{boolean ready();}
    private void await(Condition condition) throws Exception {long until=System.currentTimeMillis()+8000;while(System.currentTimeMillis()<until){boolean[] ok={false};instrumentation.runOnMainSync(()->ok[0]=condition.ready());if(ok[0])return;Thread.sleep(40);}fail("Expected safe availability state not reached");}
    private void message(String text) throws Exception {await(()->((TextView)main.findViewById(R.id.availabilityMessage)).getText().toString().contains(text));}
    private void onlineAction(){instrumentation.runOnMainSync(()->{Slider slider=main.findViewById(R.id.availabilitySlider);Bundle arguments=new Bundle();arguments.putFloat(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,100);assertNotNull(ViewCompat.getAccessibilityNodeProvider(slider));assertTrue(ViewCompat.getAccessibilityNodeProvider(slider).performAction(0,android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId(),arguments));});}
    private void click(int id){instrumentation.runOnMainSync(()->main.findViewById(id).performClick());}
    @Test public void requiresAuthoritativeProfileIdentityVehicleAndRevision() throws Exception {
        Server s=new Server();s.registration="draft";launch(s);message("после одобрения");instrumentation.runOnMainSync(()->assertFalse(main.findViewById(R.id.availabilitySlider).isEnabled()));assertEquals(0,s.puts.get());
        s.registration="approved";s.identity="unverified";click(R.id.availabilityReload);message("проверка личности");assertEquals(0,s.puts.get());
        s.identity="verified";s.missingRevision=true;click(R.id.availabilityReload);message("пока не готовы");instrumentation.runOnMainSync(()->assertEquals(View.GONE,main.findViewById(R.id.availabilityOffline).getVisibility()));
    }
    @Test public void onlineWaitsForServerAndOfflineKeepsVehicleWithIndependentRevision() throws Exception {
        Server s=new Server();s.entered=new CountDownLatch(1);s.release=new CountDownLatch(1);launch(s);
        instrumentation.runOnMainSync(()->assertEquals(3,((Spinner)main.findViewById(R.id.availabilityVehicle)).getCount()));onlineAction();assertTrue(s.entered.await(8,TimeUnit.SECONDS));
        instrumentation.runOnMainSync(()->assertEquals(View.GONE,main.findViewById(R.id.availabilityOnline).getVisibility()));assertEquals(1,s.puts.get());s.release.countDown();
        await(()->main.findViewById(R.id.availabilityOnline).getVisibility()==View.VISIBLE);assertEquals(7,s.lastRevision);assertEquals(CAR1,s.lastVehicle);String first=s.lastKey;
        click(R.id.availabilityGoOffline);await(()->main.findViewById(R.id.availabilityOffline).getVisibility()==View.VISIBLE);assertEquals(2,s.puts.get());assertEquals(8,s.lastRevision);assertEquals(CAR1,s.lastVehicle);assertNotEquals(first,s.lastKey);
    }
    @Test public void errorsNeverFakeRadarAndRequireExplicitReload() throws Exception {
        Server s=new Server();s.failure="vehicle_photo_control_required";launch(s);onlineAction();message("фотоконтроль");assertEquals(1,s.puts.get());
        instrumentation.runOnMainSync(()->{assertEquals(View.GONE,main.findViewById(R.id.availabilityOnline).getVisibility());assertEquals(View.GONE,main.findViewById(R.id.availabilityOffline).getVisibility());assertFalse(((TextView)main.findViewById(R.id.availabilityMessage)).getText().toString().contains("PRIVATE"));});
        s.failure="availability_preconditions_failed";s.failureStatus=409;click(R.id.availabilityReload);message("не на линии");onlineAction();message("текущего назначения");assertEquals(2,s.puts.get());
    }
    @Test public void lostAcknowledgementIsNotRetriedAndReloadResolvesServerState() throws Exception {
        Server s=new Server();s.disconnect=true;launch(s);onlineAction();message("могло сохраниться");assertEquals(1,s.puts.get());
        click(R.id.availabilityReload);await(()->main.findViewById(R.id.availabilityOnline).getVisibility()==View.VISIBLE);assertEquals(1,s.puts.get());
    }
    @Test public void delayedUnauthorizedPreservesNextLoginAndNeverReplaysAvailability() throws Exception {
        Server s=new Server();s.failure="authorization_required";s.failureStatus=401;s.entered=new CountDownLatch(1);s.release=new CountDownLatch(1);launch(s);onlineAction();assertTrue(s.entered.await(8,TimeUnit.SECONDS));
        assertTrue(store.save(session("next-login")));s.release.countDown();message("Сессия изменилась");assertEquals(1,s.puts.get());assertEquals("refresh-next-login",store.load().getRefreshToken());
    }
    @Test public void backgroundClearsVehicleAndInvalidatesPendingOperation() throws Exception {
        Server s=new Server();s.entered=new CountDownLatch(1);s.release=new CountDownLatch(1);launch(s);
        instrumentation.runOnMainSync(()->{android.util.SparseArray<android.os.Parcelable> state=new android.util.SparseArray<>();main.saveHierarchyState(state);Parcel p=Parcel.obtain();try{p.writeSparseArray(state);assertFalse(new String(p.marshall(),java.nio.charset.StandardCharsets.UTF_16LE).contains("SYNTH-001"));}finally{p.recycle();}});
        onlineAction();assertTrue(s.entered.await(8,TimeUnit.SECONDS));instrumentation.runOnMainSync(()->instrumentation.callActivityOnStop(host));s.release.countDown();instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(()->{assertEquals(View.GONE,main.findViewById(R.id.availabilityPanel).getVisibility());assertEquals(0,((Spinner)main.findViewById(R.id.availabilityVehicle)).getCount());assertTrue((host.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);});assertEquals(1,s.puts.get());
    }
    @Test public void cancelledAvailabilityCannotRunAfterNewScope() throws Exception {
        assertTrue(store.save(session("original")));Server s=new Server();RegistrationRepository r=new RegistrationRepository(context,s);long old=r.bind();r.cancel();r.bind();
        try{r.availability(old,CAR1,"on_line",7,"availability-"+UUID.randomUUID());fail();}catch(RegistrationRepository.Failure expected){assertEquals(401,expected.status);}assertEquals(0,s.puts.get());
    }
    @Test public void availability401And503AreSentOnceWithoutRefresh() throws Exception {
        assertTrue(store.save(session("original")));Server s=new Server();RegistrationRepository r=new RegistrationRepository(context,s);long scope=r.bind();
        for(int status:new int[]{401,503}){s.failure="authorization_required";s.failureStatus=status;try{r.availability(scope,CAR1,"on_line",7,"availability-"+UUID.randomUUID());fail();}catch(RegistrationRepository.Failure expected){assertEquals(status,expected.status);}}
        assertEquals(2,s.puts.get());assertEquals("refresh-original",store.load().getRefreshToken());
    }
    @Test public void realSliderGestureUsesChosenVehicleAndOnlineHidesSwitch() throws Exception {
        Server s=new Server();launch(s);instrumentation.runOnMainSync(()->((Spinner)main.findViewById(R.id.availabilityVehicle)).setSelection(2));
        instrumentation.waitForIdleSync();
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.availabilitySlider)).perform(
            new androidx.test.espresso.action.GeneralSwipeAction(androidx.test.espresso.action.Swipe.SLOW,
                view->{int[] p=new int[2];view.getLocationOnScreen(p);return new float[]{p[0]+((Slider)view).getTrackSidePadding(),p[1]+view.getHeight()/2f};},
                view->{int[] p=new int[2];view.getLocationOnScreen(p);return new float[]{p[0]+view.getWidth()-((Slider)view).getTrackSidePadding(),p[1]+view.getHeight()/2f};},
                androidx.test.espresso.action.Press.FINGER));
        await(()->main.findViewById(R.id.availabilityOnline).getVisibility()==View.VISIBLE);
        assertEquals(CAR2,s.lastVehicle);assertEquals(1,s.puts.get());instrumentation.runOnMainSync(()->assertEquals(View.GONE,main.findViewById(R.id.availabilityOffline).getVisibility()));
    }
    @Test public void nativeAvailabilityLight() throws Exception {nativeScreens(false);}
    @Test public void nativeAvailabilityDarkLargeFont() throws Exception {nativeScreens(true);}
    private void nativeScreens(boolean dark) throws Exception {
        shell("settings put system font_scale "+(dark?"1.3":"1.0"));awaitFont(dark?1.3f:1.0f);instrumentation.runOnMainSync(()->AppCompatDelegate.setDefaultNightMode(dark?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO));
        Server s=new Server();launch(s);capture(dark?"availability-offline-dark-large":"availability-offline-light");onlineAction();await(()->main.findViewById(R.id.availabilityOnline).getVisibility()==View.VISIBLE);capture(dark?"availability-online-dark-large":"availability-online-light");
    }
    private void capture(String name) throws Exception {
        instrumentation.runOnMainSync(()->{View footer=main.findViewById(R.id.availabilityPanel);assertTrue(footer.getBottom()<=main.getHeight()-main.getPaddingBottom());assertTrue((host.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);host.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);});
        try{Thread.sleep(250);Bitmap bitmap=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(bitmap);File dir=new File(context.getFilesDir(),"registration-evidence");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}}
        finally{instrumentation.runOnMainSync(()->host.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE));}
    }
    private void awaitFont(float expected) throws Exception {long until=System.currentTimeMillis()+8000;while(System.currentTimeMillis()<until){instrumentation.waitForIdleSync();if(Math.abs(context.getResources().getConfiguration().fontScale-expected)<0.01f)return;Thread.sleep(40);}fail("Fixture font configuration did not settle");}
    private void shell(String cmd) throws Exception {try(java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(instrumentation.getUiAutomation().executeShellCommand(cmd))){byte[] b=new byte[512];while(in.read(b)!=-1){ }}}
}
