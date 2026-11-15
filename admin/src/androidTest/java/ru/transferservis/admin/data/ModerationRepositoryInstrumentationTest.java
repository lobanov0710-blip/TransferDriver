package ru.transferservis.admin.data;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class ModerationRepositoryInstrumentationTest {
    private Context context;private AdminRepository repository;private ModerationServer server;private AdminSessionStore store;
    @Before public void setup() throws Exception {context=InstrumentationRegistry.getInstrumentation().getTargetContext();store=new AdminSessionStore(context);store.clear();server=new ModerationServer();repository=new AdminRepository(context,new AdminApiClient(server));repository.login("fixture","fixture-password",repository.generation());}
    @After public void cleanup(){repository.executor.shutdownNow();store.clear();}
    @Test public void partialReviewUsesFreshPermissionsAndPreservesOtherVerifiedItems() throws Exception {
        AdminRepository.ReviewResult initial=repository.review(ModerationServer.DRIVER,"admin-1");
        repository.applyReview(ReviewAction.start(initial.access,initial.registration));
        AdminRepository.ReviewResult under=repository.review(ModerationServer.DRIVER,"admin-1");
        ReviewAction action=ReviewAction.decide(under.access,under.registration,under.registration.items.get(0),"changes_required","Synthetic correction reason");
        repository.applyReview(action);AdminRepository.ReviewResult after=repository.review(ModerationServer.DRIVER,"admin-1");
        assertEquals("changes_required",after.registration.items.get(0).status);assertEquals("verified",after.registration.items.get(1).status);assertEquals(2,after.registration.history.size());assertEquals(action.key,server.lastKey);
        assertFalse(context.getSharedPreferences("admin_secure_session_v1",Context.MODE_PRIVATE).getAll().toString().contains("Synthetic correction reason"));
    }
    @Test public void lostAcknowledgementRequiresExplicitRetryWithSameBodyAndKey() throws Exception {
        AdminRepository.ReviewResult initial=repository.review(ModerationServer.DRIVER,"admin-1");ReviewAction action=ReviewAction.start(initial.access,initial.registration);server.dropAcknowledgement=true;
        try{repository.applyReview(action);fail();}catch(IOException expected){}assertEquals(1,server.posts.get());String original=server.lastBody;
        repository.applyReview(action);assertEquals(2,server.posts.get());assertEquals(original,server.lastBody);assertEquals(action.key,server.lastKey);assertEquals(1,server.history.size());
    }
    @Test public void revokedGrantStopsPendingMutationBeforePostAndSensitiveReadBeforeTransport() throws Exception {
        server.status="under_review";server.revision=2;
        AdminRepository.ReviewResult initial=repository.review(ModerationServer.DRIVER,"admin-1");ReviewAction action=ReviewAction.decide(initial.access,initial.registration,initial.registration.items.get(0),"changes_required","Synthetic reason");
        server.sensitiveGrant=false;try{repository.preview(initial.registration,initial.registration.items.get(0),"admin-1");fail();}catch(IOException expected){}assertEquals(0,server.resources.get());
        server.reviewGrant=false;try{repository.applyReview(action);fail();}catch(IOException expected){}assertEquals(0,server.posts.get());
    }
    @Test public void delayedMutationUnauthorizedDoesNotRefreshOrUseAnotherAccount() throws Exception {
        AdminRepository.ReviewResult initial=repository.review(ModerationServer.DRIVER,"admin-1");ReviewAction action=ReviewAction.start(initial.access,initial.registration);repository.executor.shutdownNow();AtomicInteger posts=new AtomicInteger(),refreshes=new AtomicInteger();
        AdminSession replacement=new AdminSession("admin-2","New Admin","next-access",System.currentTimeMillis()+600000,"next-refresh",System.currentTimeMillis()+1200000);
        repository=new AdminRepository(context,new AdminApiClient(chain->{
            if(chain.request().url().encodedPath().equals("/v1/admin/me"))return server.intercept(chain);
            if(chain.request().url().encodedPath().equals("/staff/refresh"))refreshes.incrementAndGet();posts.incrementAndGet();assertTrue(store.save(replacement));
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(401).message("fixture").body(ResponseBody.create(MediaType.get("application/json"),"{}")).build();
        }));
        try{repository.applyReview(action);fail();}catch(AdminApiClient.Failure expected){assertEquals(401,expected.status);}
        assertEquals(1,posts.get());assertEquals(0,refreshes.get());assertEquals("admin-2",store.load().accountId);
    }
    @Test public void resourceIsBoundToSnapshotAndNeverStoredInPreferences() throws Exception {
        AdminRepository.ReviewResult initial=repository.review(ModerationServer.DRIVER,"admin-1");
        byte[] bytes=repository.preview(initial.registration,initial.registration.items.get(0),"admin-1");assertNotNull(ru.transferservis.admin.ui.ReviewScreen.decode(bytes));
        AdminRepository.ReviewResult other=repository.review(ModerationServer.DRIVER,"admin-1");
        try{repository.preview(initial.registration,other.registration.items.get(0),"admin-1");fail();}catch(IOException expected){}
        assertEquals(1,server.resources.get());assertFalse(context.getSharedPreferences("admin_secure_session_v1",Context.MODE_PRIVATE).getAll().toString().contains(ModerationServer.NAME));
    }
}
