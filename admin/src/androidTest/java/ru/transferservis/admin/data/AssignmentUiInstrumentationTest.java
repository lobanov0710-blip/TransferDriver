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
import ru.transferservis.admin.ui.AssignmentActivity;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class AssignmentUiInstrumentationTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private final Context context=instrumentation.getTargetContext();
    private final AdminSessionStore store=new AdminSessionStore(context);
    private ActivityScenario<AssignmentActivity> scenario;
    private AssignmentActivity screen;
    private java.net.ProxySelector original;
    @Before public void before(){store.clear();original=java.net.ProxySelector.getDefault();java.net.ProxySelector.setDefault(new java.net.ProxySelector(){
        @Override public java.util.List<java.net.Proxy> select(java.net.URI ignored){throw new AssertionError("Unmocked assignment network forbidden");}
        @Override public void connectFailed(java.net.URI u,java.net.SocketAddress a,IOException e){ }
    });}
    @After public void after()throws Exception {try{if(scenario!=null)scenario.close();store.clear();instrumentation.runOnMainSync(()->AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));shell("settings put system font_scale 1.0");awaitFont(1f);}finally{java.net.ProxySelector.setDefault(original);}}
    private interface Condition{boolean ok();}
    private void await(Condition condition)throws Exception {long end=System.currentTimeMillis()+10000;while(System.currentTimeMillis()<end){boolean[] ok={false};instrumentation.runOnMainSync(()->ok[0]=condition.ok());if(ok[0])return;Thread.sleep(40);}fail("Expected assignment UI state not delivered");}
    private void launch(AssignmentServer server)throws Exception {
        scenario=ActivityScenario.launch(AssignmentActivity.class);scenario.onActivity(a->screen=a);await(()->message().contains("Сессия"));
        assertTrue(store.save(AssignmentServer.session("original")));Field f=AssignmentActivity.class.getDeclaredField("repository");f.setAccessible(true);AssignmentRepository r=new AssignmentRepository(context,server);
        instrumentation.runOnMainSync(()->{try{((AssignmentRepository)f.get(screen)).cancel();f.set(screen,r);}catch(IllegalAccessException error){throw new AssertionError(error);}screen.findViewById(R.id.assignmentReload).performClick();});
        await(()->rows().getChildCount()>0&&screen.findViewById(R.id.assignmentProgress).getVisibility()==View.GONE);await(()->screen.hasWindowFocus());
    }
    private LinearLayout rows(){return screen.findViewById(R.id.assignmentRows);}
    private String message(){return ((TextView)screen.findViewById(R.id.assignmentMessage)).getText().toString();}
    private void click(int id){instrumentation.runOnMainSync(()->screen.findViewById(id).performClick());}
    private void chooseRow(){instrumentation.runOnMainSync(()->{for(int i=0;i<rows().getChildCount();i++){View v=rows().getChildAt(i);if(v instanceof Button&&v.isEnabled()){v.performClick();return;}}fail("No permitted row");});}
    private void choosePair()throws Exception {chooseRow();await(()->rows().getChildCount()>1&&screen.findViewById(R.id.assignmentProgress).getVisibility()==View.GONE);chooseRow();await(()->screen.findViewById(R.id.assignmentReason).isShown());}
    private void reason(String text){instrumentation.runOnMainSync(()->((EditText)screen.findViewById(R.id.assignmentReason)).setText(text));}
    private void confirmDialog()throws Exception {await(()->!screen.hasWindowFocus());
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.assignment_yes)).inRoot(org.hamcrest.Matchers.allOf(androidx.test.espresso.matcher.RootMatchers.isDialog(),new org.hamcrest.TypeSafeMatcher<androidx.test.espresso.Root>(){
            @Override protected boolean matchesSafely(androidx.test.espresso.Root root){return root.getDecorView().hasWindowFocus();}
            @Override public void describeTo(org.hamcrest.Description d){d.appendText("focused confirmation dialog");}})).perform(androidx.test.espresso.action.ViewActions.click());}
    @Test public void explicitConfirmationRequiresReasonAndUsesCurrentOrderRevision()throws Exception {
        AssignmentServer server=new AssignmentServer();launch(server);choosePair();click(R.id.assignmentConfirm);instrumentation.runOnMainSync(()->assertNotNull(((EditText)screen.findViewById(R.id.assignmentReason)).getError()));assertEquals(0,server.commands.get());
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.assignmentReason)).perform(androidx.test.espresso.action.ViewActions.click(),androidx.test.espresso.action.ViewActions.typeText("Synthetic reason"));
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.assignmentConfirm)).perform(androidx.test.espresso.action.ViewActions.click());assertEquals(0,server.commands.get());confirmDialog();await(()->server.commands.get()==1&&message().contains("обновлены"));assertEquals(1,server.commits.get());assertEquals(7,AssignmentServer.object(server.lastBody).get("expectedRevision").getAsLong());
    }
    @Test public void uncertainUiOnlyRetriesOriginalBodyAndKeyAfterExplicitConfirmation()throws Exception {
        AssignmentServer server=new AssignmentServer();server.lostAck=true;launch(server);choosePair();reason("Synthetic unchanged reason");click(R.id.assignmentConfirm);confirmDialog();await(()->screen.findViewById(R.id.assignmentRetry).isShown());
        assertEquals(1,server.commands.get());String key=server.lastKey,body=server.lastBody;click(R.id.assignmentReload);await(()->screen.findViewById(R.id.assignmentRetry).isShown()&&screen.findViewById(R.id.assignmentProgress).getVisibility()==View.GONE);
        assertEquals(1,server.commands.get());click(R.id.assignmentRetry);confirmDialog();await(()->message().contains("обновлены"));assertEquals(2,server.commands.get());assertEquals(1,server.commits.get());assertEquals(key,server.lastKey);assertEquals(body,server.lastBody);
    }
    @Test public void conflictRequiresReloadAndPermissionRevocationNeverPosts()throws Exception {
        AssignmentServer server=new AssignmentServer();server.failure=409;launch(server);choosePair();reason("Synthetic reason");click(R.id.assignmentConfirm);confirmDialog();await(()->message().contains("изменилось"));assertEquals(1,server.commands.get());assertFalse(screen.findViewById(R.id.assignmentRetry).isShown());
        server.failure=0;click(R.id.assignmentReload);await(()->rows().getChildCount()>0);choosePair();server.canAssign=false;reason("Synthetic permission reason");click(R.id.assignmentConfirm);confirmDialog();await(()->message().contains("прав"));assertEquals(1,server.commands.get());
    }
    @Test public void pagingIsExplicitAndNeverSilentlyChoosesFirstOrderOrCandidate()throws Exception {
        AssignmentServer server=new AssignmentServer();server.paginate=true;launch(server);assertEquals(1,server.orders.get());assertFalse(screen.findViewById(R.id.assignmentForm).isShown());
        click(R.id.assignmentNext);await(()->server.orders.get()==2&&screen.findViewById(R.id.assignmentProgress).getVisibility()==View.GONE);click(R.id.assignmentPrevious);await(()->server.orders.get()==3&&screen.findViewById(R.id.assignmentProgress).getVisibility()==View.GONE);
        assertEquals(0,server.commands.get());assertFalse(screen.findViewById(R.id.assignmentForm).isShown());
    }
    @Test public void realLifecycleClearsReasonSnapshotsAndCancelsPendingOperation()throws Exception {
        AssignmentServer server=new AssignmentServer();launch(server);choosePair();reason("Unstored synthetic reason");
        instrumentation.runOnMainSync(()->{android.util.SparseArray<android.os.Parcelable> state=new android.util.SparseArray<>();screen.findViewById(android.R.id.content).saveHierarchyState(state);Parcel parcel=Parcel.obtain();
            try{parcel.writeSparseArray(state);for(String value:new String[]{"Unstored synthetic reason","Синтетический водитель","SYNTH-01"}){assertFalse(new String(parcel.marshall(),StandardCharsets.UTF_8).contains(value));assertFalse(new String(parcel.marshall(),StandardCharsets.UTF_16LE).contains(value));}}finally{parcel.recycle();}});
        server.holdPath="/v1/admin/orders/"+AssignmentServer.ORDER+"/assign";server.entered=new CountDownLatch(1);server.release=new CountDownLatch(1);click(R.id.assignmentConfirm);confirmDialog();assertTrue(server.entered.await(8,TimeUnit.SECONDS));
        scenario.moveToState(Lifecycle.State.CREATED);server.release.countDown();instrumentation.waitForIdleSync();instrumentation.runOnMainSync(()->{assertEquals("",((EditText)screen.findViewById(R.id.assignmentReason)).getText().toString());assertEquals(0,rows().getChildCount());assertFalse(screen.findViewById(R.id.assignmentForm).isShown());assertTrue((screen.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS,screen.findViewById(R.id.assignmentRoot).getImportantForAutofill());});
    }
    @Test public void ownerPhoneScopeNeverShowsOperationalHomeEntry()throws Exception {
        AssignmentServer server=new AssignmentServer();server.phoneScope=true;AdminApplication app=(AdminApplication)context.getApplicationContext();Field f=AdminApplication.class.getDeclaredField("repository");f.setAccessible(true);AdminRepository old=(AdminRepository)f.get(app),fake=new AdminRepository(context,new AdminApiClient(server));
        assertTrue(store.save(AssignmentServer.session("original")));f.set(app,fake);
        try(ActivityScenario<AdminActivity> home=ActivityScenario.launch(AdminActivity.class)){
            long end=System.currentTimeMillis()+10000;boolean[] shown={false};do{home.onActivity(a->shown[0]=a.findViewById(R.id.profile).isShown());if(shown[0])break;Thread.sleep(40);}while(System.currentTimeMillis()<end);assertTrue(shown[0]);
            home.onActivity(a->assertEquals(View.GONE,a.findViewById(R.id.assignments).getVisibility()));assertEquals(0,server.orders.get());
        }finally{f.set(app,old);fake.executor.shutdownNow();}
    }
    @Test public void reassignChoosesAnotherVehicleAndUsesOrderRevision()throws Exception {
        AssignmentServer server=new AssignmentServer();server.state="taken";server.revision=8;server.secondBlocked=false;launch(server);choosePair();reason("Synthetic replacement reason");
        instrumentation.runOnMainSync(()->assertEquals(screen.getString(R.string.assignment_reassign),((Button)screen.findViewById(R.id.assignmentConfirm)).getText().toString()));
        click(R.id.assignmentConfirm);confirmDialog();await(()->message().contains("обновлены"));assertTrue(server.lastPath.endsWith("/reassign"));assertEquals(AssignmentServer.CAR2,AssignmentServer.object(server.lastBody).get("vehicleId").getAsString());assertEquals(8,AssignmentServer.object(server.lastBody).get("expectedRevision").getAsLong());
    }
    @Test public void sameAccountReloginInvalidatesVisibleConfirmationWithoutErasingNextSession()throws Exception {
        AssignmentServer server=new AssignmentServer();launch(server);choosePair();reason("Old operator reason");click(R.id.assignmentConfirm);await(()->!screen.hasWindowFocus());
        assertTrue(store.save(AssignmentServer.session("next")));confirmDialog();await(()->message().contains("Сессия"));assertEquals(0,server.commands.get());assertEquals("refresh-next",store.load().refreshToken);
        instrumentation.runOnMainSync(()->assertEquals("",((EditText)screen.findViewById(R.id.assignmentReason)).getText().toString()));
    }
    @Test public void nativeAssignmentLight()throws Exception {captures(false);}
    @Test public void nativeAssignmentDarkLargeFont()throws Exception {captures(true);}
    private void captures(boolean dark)throws Exception {
        shell("settings put system font_scale "+(dark?"1.3":"1.0"));awaitFont(dark?1.3f:1f);instrumentation.runOnMainSync(()->AppCompatDelegate.setDefaultNightMode(dark?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO));
        AssignmentServer server=new AssignmentServer();launch(server);instrumentation.runOnMainSync(()->((com.google.android.material.appbar.MaterialToolbar)screen.findViewById(R.id.assignmentToolbar)).setSubtitle("Синтетические данные"));
        String suffix=dark?"-dark-large":"-light";capture("assignment-orders"+suffix,false);chooseRow();await(()->rows().getChildCount()>1&&screen.findViewById(R.id.assignmentProgress).getVisibility()==View.GONE);capture("assignment-options"+suffix,false);
        chooseRow();await(()->screen.findViewById(R.id.assignmentReason).isShown());reason("Синтетическая причина назначения");capture("assignment-form"+suffix,false);click(R.id.assignmentConfirm);await(()->!screen.hasWindowFocus());capture("assignment-confirm"+suffix,true);server.lostAck=true;confirmDialog();await(()->screen.findViewById(R.id.assignmentRetry).isShown());capture("assignment-uncertain"+suffix,false);
    }
    private void capture(String name,boolean dialog)throws Exception {
        Field field=AssignmentActivity.class.getDeclaredField("confirmation");field.setAccessible(true);androidx.appcompat.app.AlertDialog confirmation=(androidx.appcompat.app.AlertDialog)field.get(screen);
        instrumentation.runOnMainSync(()->{assertTrue((screen.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);screen.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            if(dialog){assertNotNull(confirmation);assertTrue((confirmation.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);confirmation.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);}});
        try{instrumentation.waitForIdleSync();instrumentation.getUiAutomation().waitForIdle(500,5000);Bitmap previous=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(previous);long until=System.currentTimeMillis()+8000,stable=System.currentTimeMillis();
            try{while(System.currentTimeMillis()<until){Thread.sleep(200);Bitmap next=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(next);if(!next.sameAs(previous))stable=System.currentTimeMillis();previous.recycle();previous=next;
                    if(System.currentTimeMillis()-stable>=1000){File directory=new File(context.getFilesDir(),"assignment-evidence");assertTrue(directory.isDirectory()||directory.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(directory,name+".png"))){assertTrue(previous.compress(Bitmap.CompressFormat.PNG,100,out));}return;}}
                fail("Native framebuffer did not settle");}finally{previous.recycle();}}
        finally{instrumentation.runOnMainSync(()->{screen.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);if(dialog)confirmation.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);});}
    }
    private void awaitFont(float value)throws Exception {long until=System.currentTimeMillis()+8000;while(System.currentTimeMillis()<until){if(Math.abs(context.getResources().getConfiguration().fontScale-value)<0.01f)return;Thread.sleep(40);}fail("Font fixture did not settle");}
    private void shell(String command)throws Exception {try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(instrumentation.getUiAutomation().executeShellCommand(command))){byte[] b=new byte[512];while(in.read(b)!=-1){}}}
}
