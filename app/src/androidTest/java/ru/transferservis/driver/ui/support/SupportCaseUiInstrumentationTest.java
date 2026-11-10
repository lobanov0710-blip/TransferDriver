package ru.transferservis.driver.ui.support;

import android.content.Context;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import ru.transferservis.driver.R;
import ru.transferservis.driver.data.model.SupportCaseDto;
import ru.transferservis.driver.data.repository.DriverV1Repository;
import ru.transferservis.driver.data.repository.SupportCaseGateway;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class SupportCaseUiInstrumentationTest {

    private static final String CASE_ID = "synthetic-support-case";

    private static final class Mutation {
        final String key;
        final String body;
        final long revision;

        Mutation(String key, String body, long revision) {
            this.key = key;
            this.body = body;
            this.revision = revision;
        }
    }

    private static final class FakeGateway implements SupportCaseGateway {
        final List<Mutation> mutations = new ArrayList<>();
        int loads;
        int revision = 7;
        boolean closed;
        boolean conflictFirst;
        boolean closeOnConflict;
        boolean networkFailure = true;

        @Override
        public void getSupportCase(
                String caseId,
                DriverV1Repository.SupportCaseCallback callback
        ) {
            assertEquals(CASE_ID, caseId);
            loads++;
            String json = "{\"id\":\"" + CASE_ID + "\","
                    + "\"category\":\"other\","
                    + "\"status\":\"" + (closed ? "closed" : "open") + "\","
                    + "\"revision\":" + revision + ",\"messages\":[]}";
            callback.onSuccess(new Gson().fromJson(json, SupportCaseDto.class));
        }

        @Override
        public void sendSupportMessage(
                String caseId,
                String message,
                long expectedRevision,
                String idempotencyKey,
                DriverV1Repository.SupportCaseCallback callback
        ) {
            assertEquals(CASE_ID, caseId);
            assertNotNull(idempotencyKey);
            assertFalse(idempotencyKey.trim().isEmpty());
            mutations.add(new Mutation(idempotencyKey, message, expectedRevision));

            if (conflictFirst && mutations.size() == 1) {
                revision = 8;
                closed = closeOnConflict;
                callback.onConflict();
            } else if (networkFailure) {
                callback.onError(0, "Synthetic network failure");
            } else {
                throw new AssertionError("Unexpected synthetic response");
            }
        }
    }

    private static ActivityScenario<SupportCaseActivity> launch(FakeGateway gateway) {
        SupportCaseGatewayFactory.setTestGateway(gateway);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return ActivityScenario.launch(SupportCaseActivity.createIntent(context, CASE_ID));
    }

    @Test
    public void offlineRetryAfterRecreationReusesMutationKeyAndRevision() {
        FakeGateway gateway = new FakeGateway();
        try (ActivityScenario<SupportCaseActivity> scenario = launch(gateway)) {
            scenario.onActivity(activity -> {
                EditText input = activity.findViewById(R.id.editSupportMessage);
                input.setText("Need help");
                activity.findViewById(R.id.buttonSendSupportMessage).performClick();
                assertEquals(1, gateway.mutations.size());
                assertEquals(7, gateway.mutations.get(0).revision);
                assertEquals(0, ((androidx.recyclerview.widget.RecyclerView)
                        activity.findViewById(R.id.recyclerSupportMessages))
                        .getAdapter().getItemCount());
            });

            scenario.recreate();

            scenario.onActivity(activity -> {
                EditText input = activity.findViewById(R.id.editSupportMessage);
                assertEquals("Need help", input.getText().toString());
                activity.findViewById(R.id.buttonSendSupportMessage).performClick();
                assertEquals(2, gateway.mutations.size());
                assertEquals(gateway.mutations.get(0).key, gateway.mutations.get(1).key);
                assertEquals(gateway.mutations.get(0).body, gateway.mutations.get(1).body);
                assertEquals(gateway.mutations.get(0).revision,
                        gateway.mutations.get(1).revision);
            });
        } finally {
            SupportCaseGatewayFactory.setTestGateway(null);
        }
    }

    @Test
    public void conflictRefreshesServerRevisionAndInvalidatesOldMutationKey() {
        FakeGateway gateway = new FakeGateway();
        gateway.conflictFirst = true;
        try (ActivityScenario<SupportCaseActivity> scenario = launch(gateway)) {
            scenario.onActivity(activity -> {
                EditText input = activity.findViewById(R.id.editSupportMessage);
                input.setText("Issue persists");
                activity.findViewById(R.id.buttonSendSupportMessage).performClick();
                assertEquals(1, gateway.mutations.size());
                assertEquals(7, gateway.mutations.get(0).revision);
                assertEquals(2, gateway.loads);

                activity.findViewById(R.id.buttonSendSupportMessage).performClick();
                assertEquals(2, gateway.mutations.size());
                assertEquals(8, gateway.mutations.get(1).revision);
                assertNotEquals(gateway.mutations.get(0).key,
                        gateway.mutations.get(1).key);
            });
        } finally {
            SupportCaseGatewayFactory.setTestGateway(null);
        }
    }

    @Test
    public void conflictWithServerSideClosureDisablesComposer() {
        FakeGateway gateway = new FakeGateway();
        gateway.conflictFirst = true;
        gateway.closeOnConflict = true;
        try (ActivityScenario<SupportCaseActivity> scenario = launch(gateway)) {
            scenario.onActivity(activity -> {
                EditText input = activity.findViewById(R.id.editSupportMessage);
                Button send = activity.findViewById(R.id.buttonSendSupportMessage);
                input.setText("Already resolved");
                send.performClick();
                assertEquals(1, gateway.mutations.size());
                assertEquals(2, gateway.loads);
                assertFalse(input.isEnabled());
                assertFalse(send.isEnabled());
                assertEquals(0, ((androidx.recyclerview.widget.RecyclerView)
                        activity.findViewById(R.id.recyclerSupportMessages))
                        .getAdapter().getItemCount());
            });
        } finally {
            SupportCaseGatewayFactory.setTestGateway(null);
        }
    }

    @Test
    public void closedSupportCaseCannotSendAndWindowRemainsSecure() {
        FakeGateway gateway = new FakeGateway();
        gateway.closed = true;
        try (ActivityScenario<SupportCaseActivity> scenario = launch(gateway)) {
            scenario.onActivity(activity -> {
                EditText input = activity.findViewById(R.id.editSupportMessage);
                Button send = activity.findViewById(R.id.buttonSendSupportMessage);
                assertFalse(input.isEnabled());
                assertFalse(send.isEnabled());
                assertEquals(0, gateway.mutations.size());
                assertTrue((activity.getWindow().getAttributes().flags
                        & WindowManager.LayoutParams.FLAG_SECURE) != 0);
            });
            scenario.recreate();
            scenario.onActivity(activity -> {
                Button send = activity.findViewById(R.id.buttonSendSupportMessage);
                assertFalse(send.isEnabled());
                assertTrue((activity.getWindow().getAttributes().flags
                        & WindowManager.LayoutParams.FLAG_SECURE) != 0);
            });
        } finally {
            SupportCaseGatewayFactory.setTestGateway(null);
        }
    }
}
