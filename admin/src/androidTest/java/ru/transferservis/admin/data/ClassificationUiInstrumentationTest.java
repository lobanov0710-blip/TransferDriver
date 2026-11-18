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
import ru.transferservis.admin.ui.VehicleClassificationActivity;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public final class ClassificationUiInstrumentationTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private final Context context=instrumentation.getTargetContext();private final AdminSessionStore store=new AdminSessionStore(context);
    private ActivityScenario<VehicleClassificationActivity> scenario;private VehicleClassificationActivity screen;
    @Before public void before(){store.clear();}
    @After public void after()throws Exception {if(scenario!=null)scenario.close();store.clear();instrumentation.runOnMainSync(()->AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM));shell("settings put system font_scale 1.0");}
    private void launch(ClassificationServer server)throws Exception {
        // No session exists during normal onStart: no real provider request can run.
        scenario=ActivityScenario.launch(VehicleClassificationActivity.intent(context,ClassificationServer.ACTOR,ClassificationServer.CAR));scenario.onActivity(a->screen=a);
        Field field=VehicleClassificationActivity.class.getDeclaredField("repository");field.setAccessible(true);
        VehicleClassificationRepository repository=new VehicleClassificationRepository(context,server);assertTrue(store.save(ClassificationServer.session("original")));
        instrumentation.runOnMainSync(()->{try{((VehicleClassificationRepository)field.get(screen)).cancel();field.set(screen,repository);screen.findViewById(R.id.classificationReload).performClick();}catch(IllegalAccessException e){throw new AssertionError(e);}});
        await(()->screen.findViewById(R.id.classificationProgress).getVisibility()==View.GONE);await(()->screen.hasWindowFocus());
    }
    private interface Condition{boolean yes();}
    private void await(Condition condition)throws Exception {long until=System.currentTimeMillis()+8000;while(System.currentTimeMillis()<until){instrumentation.waitForIdleSync();if(condition.yes())return;Thread.sleep(40);}fail("Classification UI did not settle");}
    private String text(int id){return ((TextView)screen.findViewById(id)).getText().toString();}
    private void click(int id){instrumentation.runOnMainSync(()->screen.findViewById(id).performClick());}
    @Test public void classificationUiSeparatesCurrentProofFromHistoryAndPagesWithoutMutation()throws Exception {
        ClassificationServer server=new ClassificationServer();server.paginate=true;launch(server);assertTrue(text(R.id.classificationDeclared).contains("2023"));assertTrue(text(R.id.classificationVerified).contains("Гибрид"));
        assertTrue(text(R.id.classificationResult).contains("кандидат"));assertTrue(text(R.id.classificationResult).contains("Комфорт"));assertTrue(text(R.id.classificationRule).contains("Модель правила: Toyota Camry"));
        click(R.id.classificationNext);await(()->screen.findViewById(R.id.classificationPrevious).isEnabled()&&screen.findViewById(R.id.classificationProgress).getVisibility()==View.GONE);
        assertFalse(screen.findViewById(R.id.classificationNext).isEnabled());click(R.id.classificationPrevious);await(()->screen.findViewById(R.id.classificationNext).isEnabled()&&screen.findViewById(R.id.classificationProgress).getVisibility()==View.GONE);
        server.unverified=true;click(R.id.classificationReload);await(()->text(R.id.classificationVerified).contains("Актуального подтверждения нет"));assertEquals(context.getString(R.string.classification_no_class),text(R.id.classificationResult));
        assertTrue(text(R.id.classificationHistory).contains("Кандидат"));assertTrue(text(R.id.classificationDeclared).contains("Не указано"));
        assertEquals(0,countInputs(screen.findViewById(R.id.classificationRoot)));
    }
    private int countInputs(View view){if(view instanceof EditText)return 1;int n=0;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)n+=countInputs(((ViewGroup)view).getChildAt(i));return n;}
    @Test public void classificationBackgroundClearsMetadataAndSavedHierarchy()throws Exception {
        launch(new ClassificationServer());assertTrue(text(R.id.classificationDeclared).contains("Toyota"));Bundle saved=new Bundle();
        instrumentation.runOnMainSync(()->{try{java.lang.reflect.Method method=VehicleClassificationActivity.class.getDeclaredMethod("onSaveInstanceState",Bundle.class);method.setAccessible(true);method.invoke(screen,saved);}catch(Exception e){throw new AssertionError(e);}});
        assertTrue(saved.isEmpty());assertFalse(screen.getIntent().hasExtra("vehicle"));assertFalse(screen.getIntent().hasExtra("account"));scenario.moveToState(Lifecycle.State.CREATED);
        assertEquals("",text(R.id.classificationDeclared));assertEquals("",text(R.id.classificationVerified));assertEquals("",text(R.id.classificationHistory));
        Field data=VehicleClassificationActivity.class.getDeclaredField("data");data.setAccessible(true);assertNull(data.get(screen));
        assertTrue((screen.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);
    }
    @Test public void classificationStaleReadAndPhoneScopeClearCardWithoutAutomaticReload()throws Exception {
        ClassificationServer server=new ClassificationServer();server.paginate=true;launch(server);server.revision++;int before=server.reads.get();click(R.id.classificationNext);
        await(()->text(R.id.classificationMessage).contains("Данные изменились"));assertEquals("",text(R.id.classificationDeclared));assertEquals(before+1,server.reads.get());
        server.auth.phoneScope=true;before=server.reads.get();click(R.id.classificationReload);await(()->text(R.id.classificationMessage).contains("MFA"));assertEquals(before,server.reads.get());assertEquals("",text(R.id.classificationHistory));
    }
    @Test public void classificationMfaExpiryClearsVisibleCardWithoutNetwork()throws Exception {
        ClassificationServer server=new ClassificationServer();server.auth.owner=true;server.mfaLifetime=1500;launch(server);assertTrue(text(R.id.classificationDeclared).contains("Toyota"));int before=server.requests.get();
        await(()->text(R.id.classificationDeclared).isEmpty()&&text(R.id.classificationMessage).contains("истекла"));assertEquals(before,server.requests.get());
    }
    @Test public void nativeClassificationReadLight()throws Exception {nativeEvidence(false);}
    @Test public void nativeClassificationReadDarkLargeFont()throws Exception {nativeEvidence(true);}
    private void nativeEvidence(boolean dark)throws Exception {
        shell("settings put system font_scale "+(dark?"1.3":"1.0"));await(()->Math.abs(context.getResources().getConfiguration().fontScale-(dark?1.3f:1f))<0.01f);
        instrumentation.runOnMainSync(()->AppCompatDelegate.setDefaultNightMode(dark?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO));
        ClassificationServer server=new ClassificationServer();launch(server);String suffix=dark?"-dark-large":"-light";capture("classification-current"+suffix);
        instrumentation.runOnMainSync(()->((ScrollView)screen.findViewById(R.id.classificationScroll)).fullScroll(View.FOCUS_DOWN));capture("classification-history"+suffix);
        server.unverified=true;click(R.id.classificationReload);await(()->text(R.id.classificationVerified).contains("Актуального подтверждения нет"));capture("classification-unverified"+suffix);
    }
    private void capture(String name)throws Exception {
        instrumentation.runOnMainSync(()->{assertTrue((screen.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0);screen.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);});
        try{instrumentation.waitForIdleSync();instrumentation.getUiAutomation().waitForIdle(500,5000);Bitmap previous=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(previous);long until=System.currentTimeMillis()+8000,stable=System.currentTimeMillis();
            try{while(System.currentTimeMillis()<until){Thread.sleep(200);Bitmap next=instrumentation.getUiAutomation().takeScreenshot();assertNotNull(next);if(!next.sameAs(previous))stable=System.currentTimeMillis();previous.recycle();previous=next;
                if(System.currentTimeMillis()-stable>=1000){File directory=new File(context.getFilesDir(),"classification-evidence");assertTrue(directory.isDirectory()||directory.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(directory,name+".png"))){assertTrue(previous.compress(Bitmap.CompressFormat.PNG,100,out));}return;}}
                fail("Native framebuffer did not settle");}finally{previous.recycle();}}
        finally{instrumentation.runOnMainSync(()->screen.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE));}
    }
    private void shell(String command)throws Exception {try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(instrumentation.getUiAutomation().executeShellCommand(command))){byte[] b=new byte[512];while(in.read(b)!=-1){}}}
}
