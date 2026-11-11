package ru.transferservis.driver.ui.photo;

import android.content.Context;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        boolean simulateTimeout;
        boolean failLoads;
        boolean holdLoad;
        DriverV1Repository.PhotoChecksCallback pendingLoad;
        String lastUploadKind;
        File lastUploadFile;

        @Override
        public void getPhotoChecks(DriverV1Repository.PhotoChecksCallback callback) {
            loads++;
            if (holdLoad) {
                pendingLoad = callback;
            } else if (failLoads) {
                callback.onError(0, "Synthetic refresh failure");
            } else {
                callback.onSuccess(new ArrayList<>(checks));
            }
        }

        @Override
        public void uploadPhotoCheckImage(
                DriverPhotoCheckDto check, String kind, File file,
                DriverV1Repository.UploadCallback callback) {
            uploads++;
            lastUploadKind = kind;
            lastUploadFile = file;
            if (!simulateTimeout) {
                throw new AssertionError("Unexpected photo upload");
            }
            callback.onError(0, "Synthetic timeout");
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
    public void assignedPhotoListHidesStaleItemsDuringRefreshAndFailure() {
        FakeGateway gateway = new FakeGateway();
        gateway.setUploaded();
        try {
            PhotoCheckGatewayFactory.setTestGateway(gateway);
            Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            try (ActivityScenario<PhotoChecksActivity> scenario =
                         ActivityScenario.launch(
                                 new android.content.Intent(context, PhotoChecksActivity.class))) {
                scenario.onActivity(activity -> {
                    RecyclerView list = activity.findViewById(R.id.recyclerPhotoChecks);
                    assertEquals(View.VISIBLE, list.getVisibility());
                    assertEquals(1, list.getAdapter().getItemCount());
                    gateway.holdLoad = true;
                    // onResume refresh is normally triggered after returning from detail;
                    // invoke the same private refresh method for deterministic UI testing.
                    try {
                        Method method = PhotoChecksActivity.class.getDeclaredMethod("loadChecks");
                        method.setAccessible(true);
                        method.invoke(activity);
                    } catch (ReflectiveOperationException error) {
                        throw new AssertionError(error);
                    }
                    assertEquals(View.GONE, list.getVisibility());
                    assertEquals(0, list.getAdapter().getItemCount());
                    assertNotNull(gateway.pendingLoad);
                    gateway.pendingLoad.onError(0, "Synthetic list refresh failure");
                    assertEquals(View.GONE, list.getVisibility());
                    assertEquals(0, list.getAdapter().getItemCount());
                    assertEquals(View.VISIBLE,
                            activity.findViewById(R.id.buttonPhotoChecksRetry).getVisibility());
                });
            }
        } finally {
            PhotoCheckGatewayFactory.setTestGateway(null);
        }
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
    public void refreshFailureDisablesPreviouslyAvailablePhotoActions() {
        FakeGateway gateway = new FakeGateway();
        gateway.setUploaded();
        try (ActivityScenario<PhotoCheckActivity> scenario = launch(gateway)) {
            scenario.onActivity(activity -> {
                Button left = activity.findViewById(R.id.buttonPhotoLeft);
                assertTrue(left.isEnabled());
                gateway.failLoads = true;
                activity.findViewById(R.id.buttonPhotoCheckRefresh).performClick();
                assertFalse(left.isEnabled());
                assertFalse(((Button) activity.findViewById(R.id.buttonPhotoFront))
                        .isEnabled());
                assertEquals(0, gateway.uploads);
            });
        } finally {
            PhotoCheckGatewayFactory.setTestGateway(null);
        }
    }

    @Test
    public void refreshDisablesActionsWhileServerResponseIsPending() {
        FakeGateway gateway = new FakeGateway();
        gateway.setUploaded();
        try (ActivityScenario<PhotoCheckActivity> scenario = launch(gateway)) {
            scenario.onActivity(activity -> {
                Button left = activity.findViewById(R.id.buttonPhotoLeft);
                assertTrue(left.isEnabled());
                gateway.holdLoad = true;
                activity.findViewById(R.id.buttonPhotoCheckRefresh).performClick();
                assertFalse(left.isEnabled());
                assertNotNull(gateway.pendingLoad);
                gateway.pendingLoad.onSuccess(new ArrayList<>(gateway.checks));
                assertTrue(left.isEnabled());
                assertEquals(0, gateway.uploads);
            });
        } finally {
            PhotoCheckGatewayFactory.setTestGateway(null);
        }
    }

    @Test
    public void cancelledCameraResultDeletesTemporaryCaptureWithoutUpload() {
        FakeGateway gateway = new FakeGateway();
        gateway.setUploaded();
        final File[] capture = new File[1];
        try (ActivityScenario<PhotoCheckActivity> scenario = launch(gateway)) {
            scenario.onActivity(activity -> {
                capture[0] = createPendingFile(activity);
                try {
                    Field kindField = PhotoCheckActivity.class.getDeclaredField("captureKind");
                    Field fileField = PhotoCheckActivity.class.getDeclaredField("captureFile");
                    kindField.setAccessible(true);
                    fileField.setAccessible(true);
                    kindField.set(activity, "left");
                    fileField.set(activity, capture[0]);
                    Method result = PhotoCheckActivity.class
                            .getDeclaredMethod("onCameraResult", boolean.class);
                    result.setAccessible(true);
                    result.invoke(activity, false);
                } catch (ReflectiveOperationException error) {
                    throw new AssertionError(error);
                }
                assertFalse(capture[0].exists());
                assertEquals(0, gateway.uploads);
                assertTrue(((Button) activity.findViewById(R.id.buttonPhotoLeft))
                        .isEnabled());
            });
        } finally {
            SupportCleanup.delete(capture[0]);
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
    private static File createPendingFile(PhotoCheckActivity activity) {
        try {
            File directory = new File(activity.getCacheDir(), "photo-control");
            assertTrue(directory.isDirectory() || directory.mkdirs());
            File file = File.createTempFile("sanitized_test_", ".jpg", directory);
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(new byte[]{1, 2, 3, 4});
            }
            return file;
        } catch (IOException error) {
            throw new AssertionError(error);
        }
    }

    private static void setPending(PhotoCheckActivity activity, String kind, File file) {
        try {
            Field kindField = PhotoCheckActivity.class.getDeclaredField("pendingUploadKind");
            Field fileField = PhotoCheckActivity.class.getDeclaredField("pendingUploadFile");
            kindField.setAccessible(true);
            fileField.setAccessible(true);
            kindField.set(activity, kind);
            fileField.set(activity, file);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    @Test
    public void pendingPrivateFileSurvivesRecreationAndTimeoutForExplicitRetry() {
        FakeGateway gateway = new FakeGateway();
        gateway.simulateTimeout = true;
        gateway.setUploaded();
        final File[] pending = new File[1];
        try (ActivityScenario<PhotoCheckActivity> scenario = launch(gateway)) {
            scenario.onActivity(activity -> {
                pending[0] = createPendingFile(activity);
                setPending(activity, "left", pending[0]);
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                Button left = activity.findViewById(R.id.buttonPhotoLeft);
                assertTrue(pending[0].isFile());
                assertTrue(left.isEnabled());
                assertTrue(left.getText().toString().contains(
                        activity.getString(R.string.photo_kind_left)));
                assertEquals(0, gateway.uploads);
                left.performClick();
                assertEquals(1, gateway.uploads);
                assertEquals("left", gateway.lastUploadKind);
                assertEquals(pending[0].getAbsolutePath(),
                        gateway.lastUploadFile.getAbsolutePath());
                assertTrue(pending[0].isFile());
                assertTrue(left.isEnabled());
                assertTrue(gateway.loads >= 3);
            });
        } finally {
            SupportCleanup.delete(pending[0]);
            PhotoCheckGatewayFactory.setTestGateway(null);
        }
    }

    @Test
    public void serverAcknowledgementClearsPendingFileAfterRecreation() {
        FakeGateway gateway = new FakeGateway();
        gateway.setUploaded();
        final File[] pending = new File[1];
        try (ActivityScenario<PhotoCheckActivity> scenario = launch(gateway)) {
            scenario.onActivity(activity -> {
                pending[0] = createPendingFile(activity);
                setPending(activity, "front", pending[0]);
            });
            gateway.setUploaded("front");
            scenario.recreate();
            scenario.onActivity(activity -> {
                assertFalse(pending[0].exists());
                assertFalse(((Button) activity.findViewById(R.id.buttonPhotoFront))
                        .isEnabled());
                assertEquals(0, gateway.uploads);
            });
        } finally {
            SupportCleanup.delete(pending[0]);
            PhotoCheckGatewayFactory.setTestGateway(null);
        }
    }

    private static final class SupportCleanup {
        static void delete(File file) {
            if (file != null && file.exists()) file.delete();
        }
    }

}
