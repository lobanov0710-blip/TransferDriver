package ru.transferservis.admin.data;
import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.*;
import java.io.IOException;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public final class AssignmentRepositoryInstrumentationTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final AdminSessionStore store=new AdminSessionStore(context);
    @Before public void before(){store.clear();assertTrue(store.save(AssignmentServer.session("original")));}
    @After public void after(){store.clear();}
    private AssignmentAction action(AssignmentRepository r,long scope)throws Exception {
        AssignmentRepository.Read<Assignments.Orders> orders=r.orders(scope,"","");AssignmentRepository.Read<Assignments.Options> options=r.options(scope,orders.data.rows.get(0),"");
        return AssignmentAction.create(options.access,scope,options.data,options.data.rows.get(0),"Synthetic reason");
    }
    @Test public void uncertainCommandIsNeverReplayedUntilExplicitSameKeyRetry()throws Exception {
        AssignmentServer server=new AssignmentServer();server.lostAck=true;AssignmentRepository r=new AssignmentRepository(context,server);long scope=r.bind();AssignmentAction action=action(r,scope);
        try{r.apply(action);fail();}catch(IOException expected){}assertEquals(1,server.commands.get());assertEquals(1,server.commits.get());
        r.apply(action);assertEquals(2,server.commands.get());assertEquals(1,server.commits.get());assertEquals(action.key,server.lastKey);assertEquals(action.body(),AssignmentServer.object(server.lastBody));r.cancel();
    }
    @Test public void revokedGrantOrOwnerMfaStopsMutationBeforePost()throws Exception {
        AssignmentServer server=new AssignmentServer();AssignmentRepository r=new AssignmentRepository(context,server);long scope=r.bind();AssignmentAction action=action(r,scope);server.canAssign=false;
        try{r.apply(action);fail();}catch(AdminApiClient.Failure expected){assertEquals(403,expected.status);}assertEquals(0,server.commands.get());
        server.owner=true;server.mfa=false;try{r.apply(action);fail();}catch(AdminApiClient.Failure expected){assertEquals(403,expected.status);}assertEquals(0,server.commands.get());r.cancel();
    }
    @Test public void cancelledQueuedActionCannotRunAfterSameSessionRebind()throws Exception {
        AssignmentServer server=new AssignmentServer();AssignmentRepository r=new AssignmentRepository(context,server);long old=r.bind();AssignmentAction action=action(r,old);r.cancel();r.bind();
        try{r.apply(action);fail();}catch(AdminApiClient.Failure expected){assertEquals(401,expected.status);}assertEquals(0,server.commands.get());r.cancel();
    }
    @Test public void delayedReadCannotExposeOldAccountOrEraseItsNextLogin()throws Exception {
        AssignmentServer server=new AssignmentServer();server.holdPath="/v1/admin/me";server.entered=new CountDownLatch(1);server.release=new CountDownLatch(1);AssignmentRepository r=new AssignmentRepository(context,server);long scope=r.bind();
        FutureTask<Boolean> task=new FutureTask<>(()->{try{r.orders(scope,"","");return false;}catch(AdminApiClient.Failure expected){return expected.status==401;}});Thread thread=new Thread(task);thread.start();
        try{assertTrue(server.entered.await(8,TimeUnit.SECONDS));assertTrue(store.save(AssignmentServer.session("next")));server.release.countDown();assertTrue(task.get(8,TimeUnit.SECONDS));assertEquals("refresh-next",store.load().refreshToken);assertEquals(0,server.orders.get());}
        finally{server.release.countDown();r.cancel();thread.join(8000);}
    }
    @Test public void command401And503HaveNoAutomaticRefreshOrReplay()throws Exception {
        AssignmentServer server=new AssignmentServer();AssignmentRepository r=new AssignmentRepository(context,server);long scope=r.bind();AssignmentAction action=action(r,scope);
        for(int status:new int[]{401,503}){server.failure=status;try{r.apply(action);fail();}catch(AdminApiClient.Failure expected){assertEquals(status,expected.status);assertFalse(expected.getMessage().contains("PRIVATE"));}}
        assertEquals(2,server.commands.get());assertEquals("refresh-original",store.load().refreshToken);r.cancel();
    }
    @Test public void revisionChangeInvalidatesSelectionAndOwnerPhoneScopeHasNoOperationalRead()throws Exception {
        AssignmentServer server=new AssignmentServer();AssignmentRepository r=new AssignmentRepository(context,server);long scope=r.bind();Assignments.Order old=r.orders(scope,"","").data.rows.get(0);server.revision++;
        try{r.options(scope,old,"");fail();}catch(AdminApiClient.Failure expected){assertEquals(409,expected.status);}
        int before=server.orders.get();server.phoneScope=true;try{r.orders(scope,"","");fail();}catch(AdminApiClient.Failure expected){assertEquals(403,expected.status);}assertEquals(before,server.orders.get());r.cancel();
    }
}
