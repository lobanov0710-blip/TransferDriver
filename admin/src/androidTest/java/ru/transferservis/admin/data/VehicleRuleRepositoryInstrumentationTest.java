package ru.transferservis.admin.data;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public final class VehicleRuleRepositoryInstrumentationTest {
    final Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();final AdminSessionStore store=new AdminSessionStore(c);VehicleRuleRepository repo;VehicleRuleServer server;long scope;
    @Before public void before()throws Exception {store.clear();server=new VehicleRuleServer();repo=new VehicleRuleRepository(c,server);assertTrue(store.save(ClassificationServer.session("original")));scope=repo.bind(VehicleRuleServer.ACTOR);}
    @After public void after(){repo.cancel();store.clear();}
    VehicleRuleAction action()throws Exception {VehicleRuleRepository.Read r=repo.read(scope,"Toyota","Camry");return VehicleRuleAction.create(r.access,r.snapshot,"Toyota","Camry","2023","comfort",List.of("sedan","crossover"),List.of("Original; intact condition"),"Synthetic reason");}
    @Test public void ruleUnknownOutcomeReadAndExplicitRetryKeepOriginalVersionBodyAndKey()throws Exception {VehicleRuleAction a=action();server.lostAck=true;try{repo.apply(a);fail();}catch(IOException expected){}assertEquals(1,server.posts.get());assertEquals("rule_created001",repo.read(scope,"Toyota","Camry").snapshot.rule.version);assertEquals("rule_created001",repo.apply(a).version);assertEquals(2,server.posts.get());assertEquals(1,server.commits.get());assertEquals(a.key,server.lastKey);assertEquals(a.body().toString(),server.lastBody);}
    @Test public void ruleDispatcherErroneousGrantAndPhoneScopeAreDeniedBeforeReadOrPost()throws Exception {server.auth.owner=false;VehicleRuleAction a=unchecked();try{repo.read(scope,"Toyota","Camry");fail();}catch(AdminApiClient.Failure e){assertEquals(403,e.status);}server.auth.owner=true;server.auth.phoneScope=true;try{repo.apply(a);fail();}catch(AdminApiClient.Failure e){assertEquals(403,e.status);}assertEquals(0,server.reads.get());assertEquals(0,server.posts.get());}
    private VehicleRuleAction unchecked()throws Exception {server.auth.owner=true;VehicleRuleAction a=action();server.reads.set(0);server.auth.owner=false;return a;}
    @Test public void ruleMfaRevocationSessionReplacementAndCancellationNeverWrite()throws Exception {VehicleRuleAction a=action();server.auth.mfa=false;try{repo.apply(a);fail();}catch(AdminApiClient.Failure e){assertEquals(403,e.status);}server.auth.mfa=true;assertTrue(store.save(ClassificationServer.session("replacement")));try{repo.apply(a);fail();}catch(IOException expected){}assertEquals("synthetic-replacement",store.load().accessToken);assertTrue(store.save(ClassificationServer.session("original")));repo.cancel();try{repo.apply(a);fail();}catch(IOException expected){}assertEquals(0,server.posts.get());}
    @Test public void rule401503MalformedAckAnd409NeverRefreshReadOrReplay()throws Exception {VehicleRuleAction a=action();int reads=server.reads.get();for(int code:new int[]{401,503,409}){server.failure=code;try{repo.apply(a);fail();}catch(AdminApiClient.Failure e){assertEquals(code,e.status);assertFalse(e.getMessage().contains("PRIVATE"));}}server.failure=0;server.wrongAck=true;try{repo.apply(a);fail();}catch(IOException expected){}assertEquals(4,server.posts.get());assertEquals(1,server.commits.get());assertEquals(reads,server.reads.get());assertNotNull(store.load());}
    @Test public void ruleNewModelPreservesExplicitNoCurrentRuleAndNoDefaultMinimum()throws Exception {server.exists=false;VehicleRuleAction a=action();assertTrue(a.body().get("expectedCurrentVersion").isJsonNull());assertEquals(2023,repo.apply(a).minYear);assertEquals(1,server.commits.get());}
    @Test public void ruleLookupEscapesQueryWithoutChangingTheExactTarget()throws Exception {String model="Camry & model=Other";VehicleRuleRepository.Read r=repo.read(scope,"Toyota",model);assertEquals(model,r.snapshot.model);assertNull(r.snapshot.rule);assertEquals(1,server.reads.get());assertEquals(0,server.posts.get());}
    @Test public void ruleCancelledDelayedReadCannotExposeSnapshot()throws Exception {server.holdPath="/v1/admin/vehicle-rules";server.entered=new CountDownLatch(1);server.release=new CountDownLatch(1);ExecutorService executor=Executors.newSingleThreadExecutor();try{Future<?> f=executor.submit(()->{try{repo.read(scope,"Toyota","Camry");throw new AssertionError("Late snapshot");}catch(IOException expected){}});assertTrue(server.entered.await(5,TimeUnit.SECONDS));repo.cancel();server.release.countDown();f.get(8,TimeUnit.SECONDS);}finally{server.release.countDown();executor.shutdownNow();}assertEquals(0,server.posts.get());}
}
