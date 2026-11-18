package ru.transferservis.admin.data;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.IOException;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public final class ClassificationRepositoryInstrumentationTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();private final AdminSessionStore store=new AdminSessionStore(context);
    @Before public void before(){store.clear();assertTrue(store.save(ClassificationServer.session("original")));}
    @After public void after(){store.clear();}
    @Test public void classificationReadsFreshAccessAndNeverRequiresOrGrantsRuleManagement()throws Exception {
        ClassificationServer s=new ClassificationServer();VehicleClassificationRepository r=new VehicleClassificationRepository(context,s);long scope=r.bind(ClassificationServer.ACTOR);
        assertEquals("comfort",r.read(scope,ClassificationServer.CAR,"",null).page.decision.vehicleClass);assertEquals(1,s.rules.get());
        s.auth.read=false;assertNull(r.read(scope,ClassificationServer.CAR,"",null).rule);assertEquals(1,s.rules.get());
        s.auth.verify=false;int before=s.reads.get();try{r.read(scope,ClassificationServer.CAR,"",null);fail();}catch(AdminApiClient.Failure e){assertEquals(403,e.status);}assertEquals(before,s.reads.get());r.cancel();
    }
    @Test public void classificationOwnerMfaAndPhoneScopeDenyBeforeDataTransport()throws Exception {
        ClassificationServer s=new ClassificationServer();VehicleClassificationRepository r=new VehicleClassificationRepository(context,s);long scope=r.bind(ClassificationServer.ACTOR);
        s.auth.owner=true;s.auth.mfa=false;try{r.read(scope,ClassificationServer.CAR,"",null);fail();}catch(AdminApiClient.Failure e){assertEquals(403,e.status);}
        s.auth.mfa=true;s.auth.phoneScope=true;try{r.read(scope,ClassificationServer.CAR,"",null);fail();}catch(AdminApiClient.Failure e){assertEquals(403,e.status);}assertEquals(0,s.reads.get());r.cancel();
    }
    @Test public void classificationDelayedResponseCannotExposePriorSessionOrEraseNextLogin()throws Exception {
        ClassificationServer s=new ClassificationServer();s.holdPath="/v1/admin/vehicles/"+ClassificationServer.CAR+"/classification";s.entered=new CountDownLatch(1);s.release=new CountDownLatch(1);
        VehicleClassificationRepository r=new VehicleClassificationRepository(context,s);long scope=r.bind(ClassificationServer.ACTOR);
        FutureTask<Boolean> task=new FutureTask<>(()->{try{r.read(scope,ClassificationServer.CAR,"",null);return false;}catch(AdminApiClient.Failure e){return e.status==401;}});Thread thread=new Thread(task);thread.start();
        try{assertTrue(s.entered.await(8,TimeUnit.SECONDS));assertTrue(store.save(ClassificationServer.session("next")));s.release.countDown();assertTrue(task.get(8,TimeUnit.SECONDS));assertEquals("refresh-next",store.load().refreshToken);assertEquals(0,s.rules.get());}
        finally{s.release.countDown();r.cancel();thread.join(8000);}
    }
    @Test public void classificationCancelAndStaleRevisionCloseReadWithoutRefreshOrMutation()throws Exception {
        ClassificationServer s=new ClassificationServer();VehicleClassificationRepository r=new VehicleClassificationRepository(context,s);long old=r.bind(ClassificationServer.ACTOR);r.cancel();long scope=r.bind(ClassificationServer.ACTOR);
        try{r.read(old,ClassificationServer.CAR,"",null);fail();}catch(AdminApiClient.Failure e){assertEquals(401,e.status);}assertEquals(0,s.reads.get());
        try{r.read(scope,ClassificationServer.CAR,"history_native2",5L);fail();}catch(AdminApiClient.Failure e){assertEquals(409,e.status);}
        for(int status:new int[]{401,503}){s.failure=status;try{r.read(scope,ClassificationServer.CAR,"",null);fail();}catch(AdminApiClient.Failure e){assertEquals(status,e.status);assertFalse(e.getMessage().contains("PRIVATE"));}}
        assertEquals("refresh-original",store.load().refreshToken);r.cancel();
    }
}
