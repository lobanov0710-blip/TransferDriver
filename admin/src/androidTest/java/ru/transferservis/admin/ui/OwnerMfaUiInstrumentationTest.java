package ru.transferservis.admin.ui;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import android.widget.TextView;
import android.widget.EditText;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.transferservis.admin.R;
import ru.transferservis.admin.data.AdminSessionStore;
import ru.transferservis.admin.data.OwnerMfa;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class OwnerMfaUiInstrumentationTest {
    private static final String SEED="GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    @Before public void clear(){new AdminSessionStore(InstrumentationRegistry.getInstrumentation().getTargetContext()).clear();}
    private OwnerMfa.Enrollment enrollment() throws Exception {
        JsonObject b=new JsonObject();b.addProperty("ok",true);b.addProperty("enrollmentId","enroll_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");b.addProperty("secret",SEED);b.addProperty("expiresAt",System.currentTimeMillis()+60000);return OwnerMfa.Enrollment.parse(b);
    }
    private void assertNotSaved(AdminActivity screen,String secret){
        SparseArray<Parcelable> hierarchy=new SparseArray<>();screen.findViewById(android.R.id.content).saveHierarchyState(hierarchy);
        Parcel parcel=Parcel.obtain();try{parcel.writeSparseArray(hierarchy);byte[] bytes=parcel.marshall();assertFalse(new String(bytes,StandardCharsets.UTF_8).contains(secret));assertFalse(new String(bytes,StandardCharsets.UTF_16LE).contains(secret));}finally{parcel.recycle();}
    }
    @Test public void enrollmentBackgroundKeepsOnlyOpaqueIdsAndNeverSeed() throws Exception {
        OwnerMfa.Enrollment value=enrollment();
        try(ActivityScenario<AdminActivity> activity=ActivityScenario.launch(AdminActivity.class)){
            activity.onActivity(screen->{
                AdminViewModel model=new ViewModelProvider(screen).get(AdminViewModel.class);
                ((MutableLiveData<AdminViewModel.State>)model.state()).setValue(new AdminViewModel.State(null,Collections.emptyList(),false,false,0,null,value,Collections.emptyList()));
                assertEquals(SEED,((TextView)screen.findViewById(R.id.enrollmentSecret)).getText().toString());assertNotSaved(screen,SEED);
            });
            activity.moveToState(Lifecycle.State.CREATED);activity.moveToState(Lifecycle.State.RESUMED);
            activity.onActivity(screen->{assertEquals("",((TextView)screen.findViewById(R.id.enrollmentSecret)).getText().toString());AdminViewModel.State state=new ViewModelProvider(screen).get(AdminViewModel.class).state().getValue();assertEquals(value.id,state.enrollment.id);assertEquals(value.expiresAt,state.enrollment.expiresAt);assertEquals("",state.enrollment.secret);assertTrue(state.recoveryCodes.isEmpty());assertNull(new AdminSessionStore(screen).load());assertNotSaved(screen,value.id);});
            activity.recreate();
            activity.onActivity(screen->{assertEquals("",((TextView)screen.findViewById(R.id.enrollmentSecret)).getText().toString());assertEquals(value.id,new ViewModelProvider(screen).get(AdminViewModel.class).state().getValue().enrollment.id);});
        }
        try(ActivityScenario<AdminActivity> fresh=ActivityScenario.launch(AdminActivity.class)){
            fresh.onActivity(screen->assertFalse(new ViewModelProvider(screen).get(AdminViewModel.class).state().getValue().hasTransientSecurityData()));
        }
    }
    private OwnerMfa.Challenge challenge(long expiry) throws Exception {
        JsonObject body=new JsonObject();body.addProperty("ok",true);body.addProperty("mfaRequired",true);body.addProperty("challengeId","mfa_"+String.join("",Collections.nCopies(43,"a")));body.addProperty("expiresAt",expiry);return OwnerMfa.Challenge.parse(body);
    }
    @Test public void samePhoneSwitchPreservesChallengeWithoutCodesOrExtendedExpiry() throws Exception {
        OwnerMfa.Challenge value=challenge(System.currentTimeMillis()+60000);
        try(ActivityScenario<AdminActivity> activity=ActivityScenario.launch(AdminActivity.class)){
            activity.onActivity(screen->{AdminViewModel model=new ViewModelProvider(screen).get(AdminViewModel.class);
                ((MutableLiveData<AdminViewModel.State>)model.state()).setValue(new AdminViewModel.State(null,Collections.emptyList(),false,false,0,value,null,Collections.emptyList()));
                ((EditText)screen.findViewById(R.id.totpCode)).setText("123456");((EditText)screen.findViewById(R.id.recoveryCode)).setText("FIXTURE-CODE");assertNotSaved(screen,value.id);});
            for(int i=0;i<2;i++){activity.moveToState(Lifecycle.State.CREATED);activity.moveToState(Lifecycle.State.RESUMED);}
            activity.onActivity(screen->{AdminViewModel model=new ViewModelProvider(screen).get(AdminViewModel.class);AdminViewModel.State state=model.state().getValue();
                assertEquals(value.id,state.challenge.id);assertEquals(value.expiresAt,state.challenge.expiresAt);
                assertEquals("",((EditText)screen.findViewById(R.id.totpCode)).getText().toString());assertEquals("",((EditText)screen.findViewById(R.id.recoveryCode)).getText().toString());assertNull(new AdminSessionStore(screen).load());
                model.logout();assertFalse(model.state().getValue().hasTransientSecurityData());});
        }
    }
    @Test public void retainedChallengeExpiresWithoutAnotherNetworkRequest() throws Exception {
        CountDownLatch expired=new CountDownLatch(1);
        Observer<AdminViewModel.State> observer=state->{if(state.error==R.string.mfa_expired&&!state.hasTransientSecurityData())expired.countDown();};
        try(ActivityScenario<AdminActivity> activity=ActivityScenario.launch(AdminActivity.class)){
            OwnerMfa.Challenge value=challenge(System.currentTimeMillis()+2500);
            activity.onActivity(screen->{AdminViewModel model=new ViewModelProvider(screen).get(AdminViewModel.class);model.state().observeForever(observer);
                ((MutableLiveData<AdminViewModel.State>)model.state()).setValue(new AdminViewModel.State(null,Collections.emptyList(),false,false,0,value,null,Collections.emptyList()));model.hideSecurityFlow();});
            try{assertTrue("Original challenge deadline must clear retained IDs",expired.await(10,TimeUnit.SECONDS));}
            finally{activity.onActivity(screen->new ViewModelProvider(screen).get(AdminViewModel.class).state().removeObserver(observer));}
        }
    }
    @Test public void recoveryCodesAreNotSavedOrRetainedWhenScreenLeavesForeground(){
        List<String> codes=new ArrayList<>();for(int i=1;i<=10;i++)codes.add(String.format(java.util.Locale.ROOT,"%032X",i));
        try(ActivityScenario<AdminActivity> activity=ActivityScenario.launch(AdminActivity.class)){
            activity.onActivity(screen->{
                AdminViewModel model=new ViewModelProvider(screen).get(AdminViewModel.class);
                ((MutableLiveData<AdminViewModel.State>)model.state()).setValue(new AdminViewModel.State(null,Collections.emptyList(),false,false,0,null,null,codes));
                assertTrue(((TextView)screen.findViewById(R.id.backupCodes)).getText().toString().contains(codes.get(0)));assertNotSaved(screen,codes.get(0));
            });
            activity.moveToState(Lifecycle.State.CREATED);activity.moveToState(Lifecycle.State.RESUMED);
            activity.onActivity(screen->{assertEquals("",((TextView)screen.findViewById(R.id.backupCodes)).getText().toString());assertFalse(new ViewModelProvider(screen).get(AdminViewModel.class).state().getValue().hasTransientSecurityData());});
        }
    }
}
