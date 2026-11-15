package ru.transferservis.admin.data;

import android.app.Application;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.transferservis.admin.AdminApplication;
import ru.transferservis.admin.R;
import ru.transferservis.admin.ui.AdminActivity;
import ru.transferservis.admin.ui.AdminViewModel;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class ModerationUiInstrumentationTest {
    private AdminApplication app;private AdminRepository previous,repository;private ModerationServer server;private Field field;
    @Before public void setup() throws Exception {
        app=(AdminApplication)InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        new AdminSessionStore(app).clear();server=new ModerationServer();repository=new AdminRepository(app,new AdminApiClient(server));repository.login("fixture","fixture-password",repository.generation());
        field=AdminApplication.class.getDeclaredField("repository");field.setAccessible(true);previous=(AdminRepository)field.get(app);field.set(app,repository);
    }
    @After public void cleanup() throws Exception {field.set(app,previous);repository.executor.shutdownNow();new AdminSessionStore(app).clear();}
    private void await(ActivityScenario<AdminActivity> activity,Predicate<AdminViewModel.State> predicate) throws Exception {
        CountDownLatch ready=new CountDownLatch(1);Observer<AdminViewModel.State> observer=s->{if(predicate.test(s))ready.countDown();};
        activity.onActivity(screen->new ViewModelProvider(screen).get(AdminViewModel.class).state().observeForever(observer));
        try{assertTrue("Expected UI state was not delivered",ready.await(10,TimeUnit.SECONDS));}
        finally{activity.onActivity(screen->new ViewModelProvider(screen).get(AdminViewModel.class).state().removeObserver(observer));}
    }
    private void open(ActivityScenario<AdminActivity> activity) throws Exception {
        await(activity,s->s.access!=null&&!s.loading);
        activity.onActivity(screen->screen.findViewById(R.id.queue).performClick());await(activity,s->s.queueLoaded&&!s.loading);
        activity.onActivity(screen->{LinearLayout rows=screen.findViewById(R.id.queueItems);assertEquals(1,rows.getChildCount());rows.getChildAt(0).performClick();});
        await(activity,s->s.reviewScreen!=null&&!s.loading&&s.access!=null);
    }
    @Test public void moderationUiExplicitlyRetriesUncertainStartAndReturnsOneElementWithReason() throws Exception {
        try(ActivityScenario<AdminActivity> activity=ActivityScenario.launch(AdminActivity.class)){
            open(activity);server.dropAcknowledgement=true;
            activity.onActivity(screen->screen.findViewById(R.id.reviewStart).performClick());onView(withText(R.string.review_yes)).inRoot(isDialog()).perform(click());
            await(activity,s->s.reviewScreen!=null&&s.reviewScreen.uncertain!=null&&!s.loading);String key=server.lastKey;
            activity.onActivity(screen->screen.findViewById(R.id.reviewRetry).performClick());
            await(activity,s->s.reviewScreen!=null&&s.reviewScreen.uncertain==null&&!s.loading&&"under_review".equals(s.reviewScreen.registration.status));
            assertEquals(key,server.lastKey);assertEquals(1,server.history.size());
            activity.onActivity(screen->{((EditText)screen.findViewById(R.id.reviewNote)).setText("Synthetic correction reason");screen.findViewById(R.id.reviewReturnItem).performClick();});
            onView(withText(R.string.review_yes)).inRoot(isDialog()).perform(click());
            await(activity,s->s.reviewScreen!=null&&!s.loading&&"changes_required".equals(s.reviewScreen.registration.items.get(0).status));
            activity.onActivity(screen->{AdminViewModel.State s=new ViewModelProvider(screen).get(AdminViewModel.class).state().getValue();assertEquals("verified",s.reviewScreen.registration.items.get(1).status);assertEquals("",((EditText)screen.findViewById(R.id.reviewNote)).getText().toString());assertFalse(screen.findViewById(R.id.reviewApprove).isEnabled());});
        }
    }
    @Test public void reviewMaterialReasonAndProfileAreNotSavedAndBackgroundClearsThem() throws Exception {
        server.status="under_review";server.revision=2;
        try(ActivityScenario<AdminActivity> activity=ActivityScenario.launch(AdminActivity.class)){
            open(activity);activity.onActivity(screen->screen.findViewById(R.id.reviewPreview).performClick());
            await(activity,s->s.reviewScreen!=null&&!s.loading&&s.reviewScreen.preview!=null);
            activity.onActivity(screen->{
                ((EditText)screen.findViewById(R.id.reviewNote)).setText("Synthetic hidden reason");
                assertNotNull(((ImageView)screen.findViewById(R.id.reviewImage)).getDrawable());
                SparseArray<Parcelable> saved=new SparseArray<>();screen.findViewById(android.R.id.content).saveHierarchyState(saved);Parcel parcel=Parcel.obtain();
                try{parcel.writeSparseArray(saved);byte[] bytes=parcel.marshall();for(String secret:new String[]{ModerationServer.NAME,"Synthetic hidden reason"}){
                    assertFalse(new String(bytes,StandardCharsets.UTF_8).contains(secret));assertFalse(new String(bytes,StandardCharsets.UTF_16LE).contains(secret));}}
                finally{parcel.recycle();}
            });
            activity.moveToState(Lifecycle.State.CREATED);activity.moveToState(Lifecycle.State.RESUMED);
            activity.onActivity(screen->{assertNull(new ViewModelProvider(screen).get(AdminViewModel.class).state().getValue().reviewScreen);assertNull(((ImageView)screen.findViewById(R.id.reviewImage)).getDrawable());assertEquals("",((EditText)screen.findViewById(R.id.reviewNote)).getText().toString());});
        }
    }
}
