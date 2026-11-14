package ru.transferservis.driver.ui.support;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.transferservis.driver.R;
import ru.transferservis.driver.data.model.SupportCaseDto;
import ru.transferservis.driver.data.repository.DriverV1Repository;
import ru.transferservis.driver.data.repository.SupportCasesGateway;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class SupportCasesUiInstrumentationTest {

    private static SupportCaseDto supportCase(String id) {
        return new Gson().fromJson(
                "{\"id\":\"" + id + "\",\"category\":\"vehicle\","
                        + "\"status\":\"open\",\"revision\":1}",
                SupportCaseDto.class
        );
    }

    private static final class Mutation {
        final String category;
        final String orderId;
        final String key;

        Mutation(String category, String orderId, String key) {
            this.category = category;
            this.orderId = orderId;
            this.key = key;
        }
    }

    private static final class FakeGateway implements SupportCasesGateway {
        final List<Mutation> mutations = new ArrayList<>();
        List<SupportCaseDto> cases = Collections.emptyList();
        boolean holdList;
        DriverV1Repository.SupportCasesCallback pendingList;

        @Override
        public void getSupportCases(
                int limit, DriverV1Repository.SupportCasesCallback callback
        ) {
            assertEquals(100, limit);
            if (holdList) {
                pendingList = callback;
            } else {
                callback.onSuccess(new ArrayList<>(cases));
            }
        }

        @Override
        public void createSupportCase(
                String category, String orderId, String key,
                DriverV1Repository.SupportCaseCallback callback
        ) {
            assertNotNull(key);
            assertFalse(key.trim().isEmpty());
            mutations.add(new Mutation(category, orderId, key));
            callback.onError(0, "Synthetic timeout");
        }
    }

    private static ActivityScenario<SupportCasesActivity> launch(FakeGateway fake) {
        SupportCasesGatewayFactory.setTestGateway(fake);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return ActivityScenario.launch(
                new android.content.Intent(context, SupportCasesActivity.class)
        );
    }

    private static void refresh(SupportCasesActivity activity) {
        try {
            Method method = SupportCasesActivity.class.getDeclaredMethod("loadCases");
            method.setAccessible(true);
            method.invoke(activity);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }

    @Test
    public void failedRefreshClearsPreviouslyVisibleSupportCases() {
        FakeGateway fake = new FakeGateway();
        fake.cases = Collections.singletonList(supportCase("synthetic-own"));
        try (ActivityScenario<SupportCasesActivity> scenario = launch(fake)) {
            scenario.onActivity(activity -> {
                RecyclerView list = activity.findViewById(R.id.recyclerSupportCases);
                assertEquals(View.VISIBLE, list.getVisibility());
                assertEquals(1, list.getAdapter().getItemCount());

                fake.holdList = true;
                refresh(activity);
                assertEquals(View.GONE, list.getVisibility());
                assertEquals(0, list.getAdapter().getItemCount());
                assertNotNull(fake.pendingList);

                fake.pendingList.onError(0, "Synthetic refresh failure");
                assertEquals(View.GONE, list.getVisibility());
                assertEquals(0, list.getAdapter().getItemCount());
                assertEquals(View.VISIBLE,
                        activity.findViewById(R.id.buttonSupportCasesRetry).getVisibility());
            });
        } finally {
            SupportCasesGatewayFactory.setTestGateway(null);
        }
    }

    @Test
    public void lateResponseCannotRestoreSupersededSupportList() {
        FakeGateway fake = new FakeGateway();
        fake.holdList = true;
        try (ActivityScenario<SupportCasesActivity> scenario = launch(fake)) {
            scenario.onActivity(activity -> {
                DriverV1Repository.SupportCasesCallback first = fake.pendingList;
                assertNotNull(first);

                fake.holdList = false;
                fake.cases = Collections.singletonList(supportCase("new-list"));
                refresh(activity);
                RecyclerView list = activity.findViewById(R.id.recyclerSupportCases);
                assertEquals(1, list.getAdapter().getItemCount());

                first.onSuccess(Arrays.asList(
                        supportCase("stale-one"), supportCase("stale-two")
                ));
                assertEquals(1, list.getAdapter().getItemCount());
                assertEquals(View.VISIBLE, list.getVisibility());
            });
        } finally {
            SupportCasesGatewayFactory.setTestGateway(null);
        }
    }

    @Test
    public void supportCreateTimeoutRetriesSameCategoryAndKeyAfterRecreation() {
        FakeGateway fake = new FakeGateway();
        try (ActivityScenario<SupportCasesActivity> scenario = launch(fake)) {
            scenario.onActivity(activity ->
                    ((Button) activity.findViewById(R.id.buttonCreateSupportCase))
                            .performClick());
            onView(withText(R.string.support_category_vehicle)).perform(click());
            assertEquals(1, fake.mutations.size());
            assertEquals("vehicle", fake.mutations.get(0).category);
            assertNull(fake.mutations.get(0).orderId);

            scenario.recreate();
            scenario.onActivity(activity ->
                    ((Button) activity.findViewById(R.id.buttonCreateSupportCase))
                            .performClick());
            assertEquals(2, fake.mutations.size());
            assertEquals(fake.mutations.get(0).key, fake.mutations.get(1).key);
            assertEquals(fake.mutations.get(0).category, fake.mutations.get(1).category);
        } finally {
            SupportCasesGatewayFactory.setTestGateway(null);
        }
    }
}
