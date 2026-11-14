package ru.transferservis.admin.ui;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import android.widget.TextView;
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
    @Test public void enrollmentSecretIsNotSavedAndBackgroundPurgesVolatileFlow() throws Exception {
        OwnerMfa.Enrollment value=enrollment();
        try(ActivityScenario<AdminActivity> activity=ActivityScenario.launch(AdminActivity.class)){
            activity.onActivity(screen->{
                AdminViewModel model=new ViewModelProvider(screen).get(AdminViewModel.class);
                ((MutableLiveData<AdminViewModel.State>)model.state()).setValue(new AdminViewModel.State(null,Collections.emptyList(),false,false,0,null,value,Collections.emptyList()));
                assertEquals(SEED,((TextView)screen.findViewById(R.id.enrollmentSecret)).getText().toString());assertNotSaved(screen,SEED);
            });
            activity.moveToState(Lifecycle.State.CREATED);activity.moveToState(Lifecycle.State.RESUMED);
            activity.onActivity(screen->{assertEquals("",((TextView)screen.findViewById(R.id.enrollmentSecret)).getText().toString());assertFalse(new ViewModelProvider(screen).get(AdminViewModel.class).state().getValue().hasTransientSecurityData());});
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
