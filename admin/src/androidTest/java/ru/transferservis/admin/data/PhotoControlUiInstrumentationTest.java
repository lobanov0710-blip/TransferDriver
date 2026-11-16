package ru.transferservis.admin.data;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import ru.transferservis.admin.R;
import ru.transferservis.admin.AdminApplication;
import ru.transferservis.admin.ui.AdminActivity;
import ru.transferservis.admin.ui.PhotoControlActivity;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class PhotoControlUiInstrumentationTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private final Context context=instrumentation.getTargetContext();
    private final AdminSessionStore store=new AdminSessionStore(context);
    private ActivityScenario<PhotoControlActivity> scenario;
    private PhotoControlActivity screen;
    private java.net.ProxySelector original;
    @Before public void before(){store.clear();original=java.net.ProxySelector.getDefault();java.net.ProxySelector.setDefault(new java.net.ProxySelector(){
        @Override public java.util.List<java.net.Proxy> select(java.net.URI ignored){throw new AssertionError("Unmocked photo network forbidden");}
        @Override public void connectFailed(java.net.URI u,java.net.SocketAddress a,IOException e){ }
    });}
    @After public void after()throws Exception {try{if(scenario!=null)scenario.close();store.clear();instrumentation.runOnMainSync(()->AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));shell("settings put system font_scale 1.0");awaitFont(1f);}finally{java.net.ProxySelector.setDefault(original);}}
    private interface Condition{boolean ok();}
    private void await(Condition condition)throws Exception {long end=System.currentTimeMillis()+10000;while(System.currentTimeMillis()<end){boolean[] ok={false};instrumentation.runOnMainSync(()->ok[0]=condition.ok());if(ok[0])return;Thread.sleep(40);}fail("Expected photo UI state not delivered");}
    private void launch(PhotoControlServer server)throws Exception {
        scenario=ActivityScenario.launch(PhotoControlActivity.class);scenario.onActivity(a->screen=a);await(()->message().contains("Сессия"));
        assertTrue(store.save(PhotoControlServer.session("original")));Field f=PhotoControlActivity.class.getDeclaredField("repository");f.setAccessible(true);PhotoControlRepository r=new PhotoControlRepository(context,server);
        instrumentation.runOnMainSync(()->{try{((PhotoControlRepository)f.get(screen)).cancel();f.set(screen,r);}catch(IllegalAccessException error){throw new AssertionError(error);}screen.findViewById(R.id.photoReload).performClick();});
        await(()->rows().getChildCount()>0&&screen.findViewById(R.id.photoProgress).getVisibility()==View.GONE);await(()->screen.hasWindowFocus());
    }
    private LinearLayout rows(){return screen.findViewById(R.id.photoRows);}
    private String message(){return ((TextView)screen.findViewById(R.id.photoMessage)).getText().toString();}
    private void click(int id){instrumentation.runOnMainSync(()->screen.findViewById(id).performClick());}
    private void chooseRow(){instrumentation.runOnMainSync(()->{for(int i=0;i<rows().getChildCount();i++){View v=rows().getChildAt(i);if(v instanceof Button&&v.isEnabled()){v.performClick();return;}}fail("No permitted row");});}
    private void chooseForm()throws Exception {chooseRow();await(()->screen.findViewById(R.id.photoNew).isShown()&&screen.findViewById(R.id.photoProgress).getVisibility()==View.GONE);click(R.id.photoNew);await(()->screen.findViewById(R.id.photoReason).isShown());}
    private void fields(){instrumentation.runOnMainSync(()->{((android.widget.Spinner)screen.findViewById(R.id.photoKind)).setSelection(2);((android.widget.Spinner)screen.findViewById(R.id.photoMode)).setSelection(1);});}
    private void reason(String text){instrumentation.runOnMainSync(()->((EditText)screen.findViewById(R.id.photoReason)).setText(text));}
    private void confirmDialog()throws Exception {await(()->!screen.hasWindowFocus());
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.photo_yes)).inRoot(org.hamcrest.Matchers.allOf(androidx.test.espresso.matcher.RootMatchers.isDialog(),new org.hamcrest.TypeSafeMatcher<androidx.test.espresso.Root>(){
            @Override protected boolean matchesSafely(androidx.test.espresso.Root root){return root.getDecorView().hasWindowFocus();}
            @Override public void describeTo(org.hamcrest.Description d){d.appendText("focused confirmation dialog");}})).perform(androidx.test.espresso.action.ViewActions.click());}
    @Test public void photoExplicitKindModeReasonConfirmationAndVehicleRevision()throws Exception {
        PhotoControlServer server=new PhotoControlServer();launch(server);chooseForm();click(R.id.photoConfirm);instrumentation.runOnMainSync(()->assertNotNull(((EditText)screen.findViewById(R.id.photoReason)).getError()));assertEquals(0,server.commands.get());
        instrumentation.runOnMainSync(()->{((android.widget.Spinner)screen.findViewById(R.id.photoKind)).setSelection(1);((android.widget.Spinner)screen.findViewById(R.id.photoMode)).setSelection(2);});reason("Synthetic photo reason");await(()->((TextView)((android.widget.Spinner)screen.findViewById(R.id.photoMode)).getSelectedView()).getLineCount()>1);click(R.id.photoConfirm);assertEquals(0,server.commands.get());confirmDialog();await(()->message().contains("Список обновлён"));assertEquals(1,server.commits.get());
        assertEquals(7,PhotoControlServer.object(server.lastBody).get("expectedRevision").getAsLong());assertEquals("complaint",PhotoControlServer.object(server.lastBody).get("kind").getAsString());assertEquals("immediate",PhotoControlServer.object(server.lastBody).get("restrictionMode").getAsString());
    }
    @Test public void photoUncertainRetryKeepsOriginalBodyAndKeyAfterRead()throws Exception {
        PhotoControlServer server=new PhotoControlServer();server.lostAck=true;launch(server);chooseForm();fields();reason("Synthetic unchanged reason");click(R.id.photoConfirm);confirmDialog();await(()->screen.findViewById(R.id.photoRetry).isShown());String key=server.lastKey,body=server.lastBody;assertEquals(1,server.commands.get());
        click(R.id.photoReload);await(()->screen.findViewById(R.id.photoRetry).isShown()&&screen.findViewById(R.id.photoProgress).getVisibility()==View.GONE);assertEquals(1,server.commands.get());click(R.id.photoRetry);confirmDialog();await(()->message().contains("Список обновлён"));assertEquals(2,server.commands.get());assertEquals(1,server.commits.get());assertEquals(key,server.lastKey);assertEquals(body,server.lastBody);
    }
    @Test public void photoConflictClosesSelectionAndRevokedGrantNeverPosts()throws Exception {
        PhotoControlServer server=new PhotoControlServer();server.failure=409;launch(server);chooseForm();fields();reason("Synthetic reason");click(R.id.photoConfirm);confirmDialog();await(()->message().contains("изменилось"));assertFalse(screen.findViewById(R.id.photoRetry).isShown());assertEquals(1,server.commands.get());
        server.failure=0;click(R.id.photoReload);await(()->rows().getChildCount()>0);chooseForm();fields();server.verify=false;reason("Synthetic reason");click(R.id.photoConfirm);confirmDialog();await(()->message().contains("прав"));assertEquals(1,server.commands.get());
    }
    @Test public void photoPagingDoesNotAutoSelectAndReadOnlyGrantCannotCreate()throws Exception {
        PhotoControlServer server=new PhotoControlServer();server.paginate=true;server.verify=false;launch(server);assertFalse(screen.findViewById(R.id.photoNew).isShown());click(R.id.photoNext);await(()->server.vehicles.get()==2&&screen.findViewById(R.id.photoProgress).getVisibility()==View.GONE);assertEquals(0,server.commands.get());
        instrumentation.runOnMainSync(()->assertFalse(rows().getChildAt(0).isEnabled()));
    }
    @Test public void photoSameAccountReloginInvalidatesConfirmationAndPreservesNextSession()throws Exception {
        PhotoControlServer server=new PhotoControlServer();launch(server);chooseForm();fields();reason("Private synthetic reason");click(R.id.photoConfirm);await(()->!screen.hasWindowFocus());assertTrue(store.save(PhotoControlServer.session("next")));confirmDialog();await(()->message().contains("Сессия"));assertEquals(0,server.commands.get());assertEquals("refresh-next",store.load().refreshToken);
    }
    @Test public void photoBackgroundClearsReasonSnapshotsAndSavedViewState()throws Exception {
        PhotoControlServer server=new PhotoControlServer();launch(server);chooseForm();fields();reason("Unstored synthetic photo reason");
        instrumentation.runOnMainSync(()->{android.util.SparseArray<android.os.Parcelable> state=new android.util.SparseArray<>();screen.findViewById(android.R.id.content).saveHierarchyState(state);Parcel parcel=Parcel.obtain();try{parcel.writeSparseArray(state);for(String v:new String[]{"Unstored synthetic photo reason","SYNTH-01"}){assertFalse(new String(parcel.marshall(),StandardCharsets.UTF_8).contains(v));assertFalse(new String(parcel.marshall(),StandardCharsets.UTF_16LE).contains(v));}}finally{parcel.recycle();}});
        server.holdPath="/v1/admin/vehicles/"+PhotoControlServer.CAR+"/photo-checks";server.entered=new CountDownLatch(1);server.release=new CountDownLatch(1);click(R.id.photoConfirm);confirmDialog();assertTrue(server.entered.await(8,TimeUnit.SECONDS));scenario.moveToState(Lifecycle.State.CREATED);server.release.countDown();instrumentation.waitForIdleSync();
        instrumentation.runOnMainSync(()->{assertEquals("",((EditText)screen.findViewById(R.id.photoReason)).getText().toString());assertEquals(0,rows().getChildCount());assertFalse(screen.findViewById(R.id.photoForm).isShown());assertTrue((screen.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);});
    }
    @Test public void nativePhotoControlLight()throws Exception {captures(false);}
    @Test public void nativePhotoControlDarkLargeFont()throws Exception {captures(true);}
    private void captures(boolean dark)throws Exception {
        shell("settings put system font_scale "+(dark?"1.3":"1.0"));awaitFont(dark?1.3f:1f);instrumentation.runOnMainSync(()->AppCompatDelegate.setDefaultNightMode(dark?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO));
        PhotoControlServer server=new PhotoControlServer();launch(server);instrumentation.runOnMainSync(()->((com.google.android.material.appbar.MaterialToolbar)screen.findViewById(R.id.photoToolbar)).setSubtitle("Синтетические данные"));String suffix=dark?"-dark-large":"-light";
        capture("photo-vehicles"+suffix,false);chooseRow();await(()->screen.findViewById(R.id.photoNew).isShown());capture("photo-checks"+suffix,false);click(R.id.photoNew);await(()->screen.findViewById(R.id.photoReason).isShown());fields();reason("Синтетическая причина фотоконтроля");capture("photo-form"+suffix,false);click(R.id.photoConfirm);await(()->!screen.hasWindowFocus());capture("photo-confirm"+suffix,true);server.lostAck=true;confirmDialog();await(()->screen.findViewById(R.id.photoRetry).isShown());capture("photo-uncertain"+suffix,false);
    }
    private void capture(String name,boolean dialog)throws Exception {
        Field field=PhotoControlActivity.class.getDeclaredField("confirmation");field.setAccessible(true);androidx.appcompat.app.AlertDialog confirmation=(androidx.appcompat.app.AlertDialog)field.get(screen);
        instrumentation.runOnMainSync(()->{assertTrue((screen.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);screen.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            if(dialog){assertNotNull(confirmation);assertTrue((confirmation.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);confirmation.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);}});
        try{instrumentation.waitForIdleSync();instrumentation.getUiAutomation().waitForIdle(500,5000);Bitmap previous=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(previous);long until=System.currentTimeMillis()+8000,stable=System.currentTimeMillis();
            try{while(System.currentTimeMillis()<until){Thread.sleep(200);Bitmap next=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(next);if(!next.sameAs(previous))stable=System.currentTimeMillis();previous.recycle();previous=next;
                    if(System.currentTimeMillis()-stable>=1000){File directory=new File(context.getFilesDir(),"photo-evidence");assertTrue(directory.isDirectory()||directory.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(directory,name+".png"))){assertTrue(previous.compress(Bitmap.CompressFormat.PNG,100,out));}return;}}
                fail("Native framebuffer did not settle");}finally{previous.recycle();}}
        finally{instrumentation.runOnMainSync(()->{screen.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);if(dialog)confirmation.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);});}
    }
    private void awaitFont(float value)throws Exception {long until=System.currentTimeMillis()+8000;while(System.currentTimeMillis()<until){if(Math.abs(context.getResources().getConfiguration().fontScale-value)<0.01f)return;Thread.sleep(40);}fail("Font fixture did not settle");}
    private void shell(String command)throws Exception {try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(instrumentation.getUiAutomation().executeShellCommand(command))){byte[] b=new byte[512];while(in.read(b)!=-1){}}}
}
