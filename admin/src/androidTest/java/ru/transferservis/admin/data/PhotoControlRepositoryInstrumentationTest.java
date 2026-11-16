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
public final class PhotoControlRepositoryInstrumentationTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private final AdminSessionStore store=new AdminSessionStore(context);
    @Before public void photoBefore(){store.clear();assertTrue(store.save(PhotoControlServer.session("original")));}
    @After public void photoAfter(){store.clear();}
    private PhotoCheckAction action(PhotoControlRepository r,long scope)throws Exception {
        PhotoControlRepository.Read<PhotoControl.Vehicles> vehicles=r.vehicles(scope,"","");PhotoControlRepository.Read<PhotoControl.Checks> checks=r.checks(scope,vehicles.data.rows.get(0),"");
        return PhotoCheckAction.create(checks.access,scope,checks.data,"manual","after_current_trip","Synthetic reason");
    }
    @Test public void photoUncertainCommandIsNeverReplayedUntilExplicitSameKeyRetry()throws Exception {
        PhotoControlServer server=new PhotoControlServer();server.lostAck=true;PhotoControlRepository r=new PhotoControlRepository(context,server);long scope=r.bind();PhotoCheckAction action=action(r,scope);
        try{r.apply(action);fail();}catch(IOException expected){}assertEquals(1,server.commands.get());assertEquals(1,server.commits.get());
        r.apply(action);assertEquals(2,server.commands.get());assertEquals(1,server.commits.get());assertEquals(action.key,server.lastKey);assertEquals(action.body(),PhotoControlServer.object(server.lastBody));r.cancel();
    }
    @Test public void photoRevokedGrantOrOwnerMfaStopsMutationBeforePost()throws Exception {
        PhotoControlServer server=new PhotoControlServer();PhotoControlRepository r=new PhotoControlRepository(context,server);long scope=r.bind();PhotoCheckAction action=action(r,scope);server.verify=false;
        try{r.apply(action);fail();}catch(AdminApiClient.Failure expected){assertEquals(403,expected.status);}assertEquals(0,server.commands.get());
        server.owner=true;server.mfa=false;try{r.apply(action);fail();}catch(AdminApiClient.Failure expected){assertEquals(403,expected.status);}assertEquals(0,server.commands.get());r.cancel();
    }
    @Test public void photoCancelledQueuedActionCannotRunAfterSameSessionRebind()throws Exception {
        PhotoControlServer server=new PhotoControlServer();PhotoControlRepository r=new PhotoControlRepository(context,server);long old=r.bind();PhotoCheckAction action=action(r,old);r.cancel();r.bind();
        try{r.apply(action);fail();}catch(AdminApiClient.Failure expected){assertEquals(401,expected.status);}assertEquals(0,server.commands.get());r.cancel();
    }
    @Test public void photoDelayedReadCannotExposeOldAccountOrEraseItsNextLogin()throws Exception {
        PhotoControlServer server=new PhotoControlServer();server.holdPath="/v1/admin/me";server.entered=new CountDownLatch(1);server.release=new CountDownLatch(1);PhotoControlRepository r=new PhotoControlRepository(context,server);long scope=r.bind();
        FutureTask<Boolean> task=new FutureTask<>(()->{try{r.vehicles(scope,"","");return false;}catch(AdminApiClient.Failure expected){return expected.status==401;}});Thread thread=new Thread(task);thread.start();
        try{assertTrue(server.entered.await(8,TimeUnit.SECONDS));assertTrue(store.save(PhotoControlServer.session("next")));server.release.countDown();assertTrue(task.get(8,TimeUnit.SECONDS));assertEquals("refresh-next",store.load().refreshToken);assertEquals(0,server.vehicles.get());}
        finally{server.release.countDown();r.cancel();thread.join(8000);}
    }
    @Test public void photoCommand401And503HaveNoAutomaticRefreshOrReplay()throws Exception {
        PhotoControlServer server=new PhotoControlServer();PhotoControlRepository r=new PhotoControlRepository(context,server);long scope=r.bind();PhotoCheckAction action=action(r,scope);
        for(int status:new int[]{401,503}){server.failure=status;try{r.apply(action);fail();}catch(AdminApiClient.Failure expected){assertEquals(status,expected.status);assertFalse(expected.getMessage().contains("PRIVATE"));}}
        assertEquals(2,server.commands.get());assertEquals("refresh-original",store.load().refreshToken);r.cancel();
    }
    @Test public void photoRevisionChangeInvalidatesSelectionAndOwnerPhoneScopeHasNoOperationalRead()throws Exception {
        PhotoControlServer server=new PhotoControlServer();PhotoControlRepository r=new PhotoControlRepository(context,server);long scope=r.bind();PhotoControl.Vehicle old=r.vehicles(scope,"","").data.rows.get(0);server.revision++;
        try{r.checks(scope,old,"");fail();}catch(AdminApiClient.Failure expected){assertEquals(409,expected.status);}
        int before=server.vehicles.get();server.phoneScope=true;try{r.vehicles(scope,"","");fail();}catch(AdminApiClient.Failure expected){assertEquals(403,expected.status);}assertEquals(before,server.vehicles.get());r.cancel();
    }
}
