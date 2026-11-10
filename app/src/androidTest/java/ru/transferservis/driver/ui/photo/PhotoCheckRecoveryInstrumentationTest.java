package ru.transferservis.driver.ui.photo;

import android.content.Context;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.transferservis.driver.R;
import ru.transferservis.driver.data.model.DriverPhotoCheckDto;
import ru.transferservis.driver.data.repository.DriverV1Repository;
import ru.transferservis.driver.data.repository.PhotoCheckGateway;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class PhotoCheckRecoveryInstrumentationTest {
    private static final String CHECK_ID = "synthetic-photo-check";

    private static final class FakeGateway implements PhotoCheckGateway {
        final List<DriverPhotoCheckDto> checks = new ArrayList<>();
        int loads;
        int uploads;

        @Override
        public void getPhotoChecks(DriverV1Repository.PhotoChecksCallback callback) {
            loads++;
            callback.onSuccess(new ArrayList<>(checks));
        }

        @Override
        public void uploadPhotoCheckImage(
                DriverPhotoCheckDto check, String kind, File file,
                DriverV1Repository.UploadCallback callback) {
            uploads++;
            throw new AssertionError("Photo upload must not occur during state recovery");
        }

        void setUploaded(String... kinds) {
            StringBuilder json = new StringBuilder();
            for (String kind : kinds) {
                if (json.length() != 0) json.append(',');
                json.append('"').append(kind).append('"');
            }
            String body = "{\"id\":\"" + CHECK_ID + "\","
                    + "\"vehicleId\":\"synthetic-vehicle\","
                    + "\"reason\":\"Synthetic verification\","
                    + "\"result\":\"pending\","
                    + "\"uploadedKindCount\":" + kinds.length + ","
                    + "\"requiredKindCount\":7,"
                    + "\"uploadedKinds\":[" + json + "]}";
            checks.clear();
            checks.add(new Gson().fromJson(body, DriverPhotoCheckDto.class));
        }
    }

    private static ActivityScenario<PhotoCheckActivity> launch(FakeGateway gateway) {
        PhotoCheckGatewayFactory.setTestGateway(gateway);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return ActivityScenario.launch(PhotoCheckActivity.createIntent(context, CHECK_ID));
    }

    @Test
    public void uploadedKindsComeFromServerAfterRecreation() {
        FakeGateway gateway = new FakeGateway();
        gateway.setUploaded("left", "front");
        try (ActivityScenario<PhotoCheckActivity> scenario = launch(gateway)) {
            scenario.onActivity(activity -> {
                assertFalse(((Button) activity.findViewById(R.id.buttonPhotoLeft)).isEnabled());
                assertFalse(((Button) activity.findViewById(R.id.buttonPhotoFront)).isEnabled());
                assertTrue(((Button) activity.findViewById(R.id.buttonPhotoRight)).isEnabled());
                assertTrue((activity.getWindow().getAttributes().flags
                        & WindowManager.LayoutParams.FLAG_SECURE) != 0);
            });
            gateway.setUploaded("left", "front", "right");
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertFalse(((Button) activity.findViewById(R.id.buttonPhotoRight)).isEnabled());
                assertTrue(((Button) activity.findViewById(R.id.buttonPhotoRear)).isEnabled());
                assertEquals(0, gateway.uploads);
                assertTrue(gateway.loads >= 2);
            });
        } finally {
            PhotoCheckGatewayFactory.setTestGateway(null);
        }
    }

    @Test
    public void missingAssignedCheckNeverDisplaysCompleted() {
        FakeGateway gateway = new FakeGateway();
        try (ActivityScenario<PhotoCheckActivity> scenario = launch(gateway)) {
            scenario.onActivity(activity -> {
                assertEquals(activity.getString(R.string.photo_control_load_failed),
                        ((TextView) activity.findViewById(R.id.textPhotoCheckDetailStatus))
                                .getText().toString());
                assertFalse(((Button) activity.findViewById(R.id.buttonPhotoLeft)).isEnabled());
                assertFalse(((Button) activity.findViewById(R.id.buttonPhotoFront)).isEnabled());
                assertEquals(0, gateway.uploads);
            });
        } finally {
            PhotoCheckGatewayFactory.setTestGateway(null);
        }
    }
}
