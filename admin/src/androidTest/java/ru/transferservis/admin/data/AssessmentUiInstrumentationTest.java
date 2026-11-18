package ru.transferservis.admin.data;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import ru.transferservis.admin.R;
import ru.transferservis.admin.ui.VehicleAssessmentActivity;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public final class AssessmentUiInstrumentationTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private final Context context=instrumentation.getTargetContext();private final AdminSessionStore store=new AdminSessionStore(context);
    private ActivityScenario<VehicleAssessmentActivity> scenario;private VehicleAssessmentActivity screen;
    @Before public void before(){store.clear();}
    @After public void after()throws Exception {if(scenario!=null)scenario.close();store.clear();instrumentation.runOnMainSync(()->AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));shell("settings put system font_scale 1.0");}
    private void launch(AssessmentServer server)throws Exception {
        // No session exists during normal onStart: no real provider request can run.
        scenario=ActivityScenario.launch(VehicleAssessmentActivity.intent(context,AssessmentServer.ACTOR,AssessmentServer.DRIVER,AssessmentServer.CAR));scenario.onActivity(a->screen=a);
        Field field=VehicleAssessmentActivity.class.getDeclaredField("repository");field.setAccessible(true);
        VehicleAssessmentRepository repository=new VehicleAssessmentRepository(context,server);assertTrue(store.save(ClassificationServer.session("original")));
        instrumentation.runOnMainSync(()->{try{((VehicleAssessmentRepository)field.get(screen)).cancel();field.set(screen,repository);screen.findViewById(R.id.assessmentReload).performClick();}catch(IllegalAccessException e){throw new AssertionError(e);}});
        await(()->screen.findViewById(R.id.assessmentProgress).getVisibility()==View.GONE);await(()->screen.hasWindowFocus());
    }
    private interface Condition{boolean yes();}
    private void await(Condition condition)throws Exception {long until=System.currentTimeMillis()+8000;while(System.currentTimeMillis()<until){instrumentation.waitForIdleSync();if(condition.yes())return;Thread.sleep(40);}fail("Assessment UI did not settle");}
    private String text(int id){return ((TextView)screen.findViewById(id)).getText().toString();}
    private void click(int id){instrumentation.runOnMainSync(()->screen.findViewById(id).performClick());}
    private void ready()throws Exception {await(()->screen.findViewById(R.id.assessmentForm).getVisibility()==View.VISIBLE&&screen.findViewById(R.id.assessmentProgress).getVisibility()==View.GONE);}
    private void fill(boolean exception)throws Exception {instrumentation.runOnMainSync(()->{if(exception){((CheckBox)screen.findViewById(R.id.assessmentException)).setChecked(true);((EditText)screen.findViewById(R.id.assessmentBasis)).setText("Camry");}((Spinner)screen.findViewById(R.id.assessmentFront)).setSelection(1);((Spinner)screen.findViewById(R.id.assessmentBack)).setSelection(1);((EditText)screen.findViewById(R.id.assessmentReason)).setText("Synthetic review reason");});click(R.id.assessmentRuleLoad);await(()->screen.findViewById(R.id.assessmentProgress).getVisibility()==View.GONE&&!text(R.id.assessmentRule).isEmpty());instrumentation.runOnMainSync(()->((Spinner)((LinearLayout)screen.findViewById(R.id.assessmentConditions)).getChildAt(1)).setSelection(1));}
    private androidx.appcompat.app.AlertDialog dialog()throws Exception {Field f=VehicleAssessmentActivity.class.getDeclaredField("confirmation");f.setAccessible(true);return (androidx.appcompat.app.AlertDialog)f.get(screen);}
    private void accept()throws Exception {androidx.appcompat.app.AlertDialog d=dialog();assertNotNull(d);instrumentation.runOnMainSync(()->d.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).performClick());}
    @Test public void verifiedLiftbackIsShownAndConfirmedWithoutClientClassSelection()throws Exception {
        AssessmentServer server=new AssessmentServer();server.source.bodyType="liftback";launch(server);ready();
        assertEquals("Лифтбек",((Spinner)screen.findViewById(R.id.assessmentBody)).getSelectedItem().toString());
        fill(false);click(R.id.assessmentConfirm);assertEquals(0,server.commands.get());
        assertTrue(((TextView)dialog().findViewById(android.R.id.message)).getText().toString().contains("Лифтбек"));
        accept();await(()->server.commits.get()==1&&text(R.id.assessmentSummary).contains("Проверка записана"));
        com.google.gson.JsonObject body=ClassificationServer.json(server.lastBody);
        assertEquals("liftback",body.getAsJsonObject("parameters").get("bodyType").getAsString());assertFalse(body.has("vehicleClass"));
    }
    @Test public void assessmentConfirmationAndLostAckKeepOriginalAcrossExplicitReadRetry()throws Exception {AssessmentServer server=new AssessmentServer();server.lostAck=true;launch(server);ready();fill(false);assertEquals(0,server.commands.get());click(R.id.assessmentConfirm);assertTrue(dialog().isShowing());assertEquals(0,server.commands.get());accept();await(()->text(R.id.assessmentMessage).contains("неизвестен"));String key=server.lastKey,body=server.lastBody;assertEquals(1,server.commands.get());click(R.id.assessmentReload);await(()->screen.findViewById(R.id.assessmentProgress).getVisibility()==View.GONE);assertEquals(View.GONE,screen.findViewById(R.id.assessmentForm).getVisibility());click(R.id.assessmentRetry);assertTrue(dialog().isShowing());assertEquals(1,server.commands.get());accept();await(()->text(R.id.assessmentSummary).contains("Проверка записана"));assertEquals(2,server.commands.get());assertEquals(1,server.commits.get());assertEquals(key,server.lastKey);assertEquals(body,server.lastBody);assertTrue(text(R.id.assessmentSummary).contains("диспетчер"));}
    @Test public void assessmentEditedInputRequiresRuleReloadAndDocumentsAreExplicit()throws Exception {AssessmentServer server=new AssessmentServer();launch(server);ready();click(R.id.assessmentConfirm);assertEquals(0,server.commands.get());assertNull(dialog());fill(false);instrumentation.runOnMainSync(()->((EditText)screen.findViewById(R.id.assessmentYear)).setText("2024"));click(R.id.assessmentConfirm);assertNull(dialog());assertTrue(text(R.id.assessmentMessage).contains("загрузите правило заново"));click(R.id.assessmentRuleLoad);await(()->text(R.id.assessmentRule).contains("rule_current001"));assertEquals(1,server.ruleReads.get());assertEquals(0,server.commands.get());}
    @Test public void assessmentOwnerExceptionHasExplicitWaiversAndDispatcherHasNoMode()throws Exception {AssessmentServer server=new AssessmentServer();launch(server);ready();assertEquals(View.GONE,screen.findViewById(R.id.assessmentException).getVisibility());server.source.auth.owner=true;click(R.id.assessmentReload);ready();fill(true);instrumentation.runOnMainSync(()->{LinearLayout c=screen.findViewById(R.id.assessmentConditions);((Spinner)c.getChildAt(1)).setSelection(2);((CheckBox)c.getChildAt(2)).setChecked(true);((CheckBox)screen.findViewById(R.id.assessmentWaiveYear)).setChecked(true);});click(R.id.assessmentConfirm);accept();await(()->server.commands.get()==1&&screen.findViewById(R.id.assessmentProgress).getVisibility()==View.GONE);com.google.gson.JsonObject b=AssessmentServer.json(server.lastBody);assertEquals("exception",b.get("mode").getAsString());assertEquals("rule_native001",b.get("expectedRuleVersion").getAsString());assertEquals("rule_current001",b.getAsJsonObject("exception").get("basisRuleVersion").getAsString());assertTrue(b.getAsJsonObject("exception").get("yearBelowMinimum").getAsBoolean());assertEquals(1,b.getAsJsonObject("exception").getAsJsonArray("waivedConditionIds").size());assertEquals("Camry",b.getAsJsonObject("parameters").get("model").getAsString());}
    @Test public void assessmentBackgroundWipesInputsEvidenceDialogAndUnknownAction()throws Exception {AssessmentServer server=new AssessmentServer();launch(server);ready();fill(false);click(R.id.assessmentConfirm);assertTrue(dialog().isShowing());Bundle saved=new Bundle();instrumentation.runOnMainSync(()->{try{java.lang.reflect.Method method=VehicleAssessmentActivity.class.getDeclaredMethod("onSaveInstanceState",Bundle.class);method.setAccessible(true);method.invoke(screen,saved);}catch(Exception e){throw new AssertionError(e);}});assertTrue(saved.isEmpty());assertFalse(screen.getIntent().hasExtra("driver"));scenario.moveToState(Lifecycle.State.CREATED);assertEquals("",text(R.id.assessmentMake));assertEquals("",text(R.id.assessmentReason));assertNull(dialog());Field f=VehicleAssessmentActivity.class.getDeclaredField("selection");f.setAccessible(true);assertNull(f.get(screen));assertTrue((screen.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);assertEquals(0,server.commands.get());}
    @Test public void assessmentUnknownWriteIsDiscardedOnBackgroundWithoutAutomaticReplay()throws Exception {AssessmentServer server=new AssessmentServer();server.lostAck=true;launch(server);ready();fill(false);click(R.id.assessmentConfirm);accept();await(()->text(R.id.assessmentMessage).contains("неизвестен"));scenario.moveToState(Lifecycle.State.CREATED);Field f=VehicleAssessmentActivity.class.getDeclaredField("uncertain");f.setAccessible(true);assertNull(f.get(screen));assertEquals("",text(R.id.assessmentSummary));assertEquals(1,server.commands.get());}
    @Test public void assessmentOwnerMfaTimerWipesFormWithoutNetwork()throws Exception {AssessmentServer server=new AssessmentServer();server.source.auth.owner=true;server.source.mfaLifetime=1500;launch(server);ready();int reads=server.reads.get();await(()->text(R.id.assessmentMake).isEmpty()&&text(R.id.assessmentMessage).contains("истекло"));assertEquals(reads,server.reads.get());assertEquals(0,server.commands.get());}
    @Test public void nativeAssessmentLight()throws Exception {nativeEvidence(false);}
    @Test public void nativeAssessmentDarkLargeFont()throws Exception {nativeEvidence(true);}
    private void nativeEvidence(boolean dark)throws Exception {shell("settings put system font_scale "+(dark?"1.3":"1.0"));await(()->Math.abs(context.getResources().getConfiguration().fontScale-(dark?1.3f:1f))<0.01f);instrumentation.runOnMainSync(()->AppCompatDelegate.setDefaultNightMode(dark?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO));AssessmentServer server=new AssessmentServer();server.source.auth.owner=true;server.source.bodyType="liftback";server.lostAck=true;launch(server);ready();String suffix=dark?"-dark-large":"-light";capture("assessment-form"+suffix);fill(true);instrumentation.runOnMainSync(()->((ScrollView)screen.findViewById(R.id.assessmentScroll)).fullScroll(View.FOCUS_DOWN));capture("assessment-conditions"+suffix);click(R.id.assessmentConfirm);capture("assessment-confirm"+suffix);accept();await(()->text(R.id.assessmentMessage).contains("неизвестен"));capture("assessment-uncertain"+suffix);click(R.id.assessmentRetry);accept();await(()->text(R.id.assessmentSummary).contains("Проверка записана"));capture("assessment-recorded"+suffix);}
    private void capture(String name)throws Exception {
        // Capture the normal review state after leaving text entry. A focused
        // editable cursor/IME is animated even with system animations disabled.
        instrumentation.runOnMainSync(()->{View root=screen.findViewById(R.id.assessmentRoot);root.setFocusableInTouchMode(true);root.requestFocus();((android.view.inputmethod.InputMethodManager)screen.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0);assertFalse(screen.getCurrentFocus() instanceof EditText);});
        androidx.appcompat.app.AlertDialog visible=dialog();boolean showing=visible!=null&&visible.isShowing();
        instrumentation.runOnMainSync(()->{assertTrue((screen.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);screen.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);if(showing){assertTrue((visible.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);visible.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);}});
        try{instrumentation.waitForIdleSync();instrumentation.getUiAutomation().waitForIdle(500,5000);Bitmap previous=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(previous);long until=System.currentTimeMillis()+8000,stable=System.currentTimeMillis();
            try{while(System.currentTimeMillis()<until){Thread.sleep(200);Bitmap next=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(next);if(!next.sameAs(previous))stable=System.currentTimeMillis();previous.recycle();previous=next;
                if(System.currentTimeMillis()-stable>=1000){File directory=new File(context.getFilesDir(),"assessment-evidence");assertTrue(directory.isDirectory()||directory.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(directory,name+".png"))){assertTrue(previous.compress(Bitmap.CompressFormat.PNG,100,out));}return;}}
                File dir=new File(context.getFilesDir(),"assessment-diagnostics");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,name+"-unstable.png"))){assertTrue(previous.compress(Bitmap.CompressFormat.PNG,100,out));}fail("Native framebuffer did not settle: "+name);}finally{previous.recycle();}}
        finally{instrumentation.runOnMainSync(()->{screen.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);if(showing)visible.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);});}
    }
    private void shell(String command)throws Exception {try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(instrumentation.getUiAutomation().executeShellCommand(command))){byte[] b=new byte[512];while(in.read(b)!=-1){}}}
}
