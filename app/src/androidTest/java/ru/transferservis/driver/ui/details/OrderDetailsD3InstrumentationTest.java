package ru.transferservis.driver.ui.details;

import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.transferservis.driver.R;
import ru.transferservis.driver.data.api.IdempotencyKeyFactory;
import ru.transferservis.driver.data.model.OrderTransitionResponse;
import ru.transferservis.driver.data.model.OrdersResponse;
import ru.transferservis.driver.data.model.StaffOrderDto;
import ru.transferservis.driver.data.repository.OrderDetailsGateway;
import ru.transferservis.driver.data.repository.OrderRepository;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class OrderDetailsD3InstrumentationTest {

    private static final String ORDER_ID = "synthetic-d3-order";

    private static StaffOrderDto order(String status, long revision) {
        return new Gson().fromJson(
                "{\"id\":\"" + ORDER_ID + "\",\"status\":\"" + status
                        + "\",\"revision\":" + revision
                        + ",\"route\":\"Synthetic route\",\"from\":\"Point A\",\"to\":\"Point B\"}",
                StaffOrderDto.class
        );
    }

    private static final class Attempt {
        final String orderId;
        final String status;
        final long revision;
        final String key;

        Attempt(String orderId, String status, long revision) {
            this.orderId = orderId;
            this.status = status;
            this.revision = revision;
            // The production OrderRepository derives the same key for this identity.
            this.key = IdempotencyKeyFactory.forTerminalDone(orderId, revision);
        }
    }

    private static final class FakeGateway implements OrderDetailsGateway {
        StaffOrderDto assignedOrder;
        boolean holdRefresh;
        OrderRepository.OrdersCallback pendingLoad;
        OrderRepository.TransitionCallback pendingTransition;
        final List<Attempt> attempts = new ArrayList<>();

        FakeGateway(StaffOrderDto order) {
            assignedOrder = order;
        }

        @Override
        public void getOrders(String status, int limit, OrderRepository.OrdersCallback callback) {
            assertNull(status);
            assertEquals(200, limit);
            if (holdRefresh) {
                pendingLoad = callback;
                return;
            }
            callback.onSuccess(OrdersResponse.assignedOnly(
                    Collections.singletonList(assignedOrder), limit, status
            ));
        }

        @Override
        public void changeOrderStatus(
                String orderId, String targetStatus, long expectedRevision,
                OrderRepository.TransitionCallback callback
        ) {
            attempts.add(new Attempt(orderId, targetStatus, expectedRevision));
            pendingTransition = callback;
        }
    }

    private static ActivityScenario<OrderDetailsActivity> launch(FakeGateway fake) {
        OrderDetailsGatewayFactory.setTestGateway(fake);
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return ActivityScenario.launch(
                OrderDetailsActivity.createIntent(context, ORDER_ID)
        );
    }

    @Test
    public void takenOrderShowsDisabledVerificationGateWithoutSelfStart() {
        FakeGateway fake = new FakeGateway(order("taken", 4));
        try (ActivityScenario<OrderDetailsActivity> scenario = launch(fake)) {
            scenario.onActivity(activity -> {
                Button action = activity.findViewById(R.id.buttonDetailsAction);
                assertEquals(View.VISIBLE, action.getVisibility());
                assertFalse(action.isEnabled());
                assertEquals(activity.getString(R.string.order_verification_required),
                        action.getText().toString());
                assertFalse(action.performClick());
                assertEquals(0, fake.attempts.size());
                assertTrue((activity.getWindow().getAttributes().flags
                        & WindowManager.LayoutParams.FLAG_SECURE) != 0);
            });
        } finally {
            OrderDetailsGatewayFactory.setTestGateway(null);
        }
    }

    @Test
    public void doneTimeoutAllowsExplicitSameRevisionRetryButNotConcurrentDuplicate() {
        FakeGateway fake = new FakeGateway(order("in_progress", 7));
        try (ActivityScenario<OrderDetailsActivity> scenario = launch(fake)) {
            scenario.onActivity(activity -> {
                Button action = activity.findViewById(R.id.buttonDetailsAction);
                assertTrue(action.isEnabled());
                action.performClick();
            });
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());

            scenario.onActivity(activity -> {
                Button action = activity.findViewById(R.id.buttonDetailsAction);
                assertEquals(1, fake.attempts.size());
                assertEquals("done", fake.attempts.get(0).status);
                assertEquals(7L, fake.attempts.get(0).revision);
                assertFalse(action.isEnabled());
                // Android disables user taps; performClick() can bypass enabled state.
                assertEquals(1, fake.attempts.size());

                fake.pendingTransition.onError(0, "Synthetic timeout");
                assertTrue(action.isEnabled());
                action.performClick();
            });
            onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());

            scenario.onActivity(activity -> {
                assertEquals(2, fake.attempts.size());
                Attempt first = fake.attempts.get(0);
                Attempt second = fake.attempts.get(1);
                assertEquals(ORDER_ID, first.orderId);
                assertEquals(first.orderId, second.orderId);
                assertEquals(first.status, second.status);
                assertEquals(first.revision, second.revision);
                assertEquals(first.key, second.key);

                fake.assignedOrder = order("done", 8);
                OrderTransitionResponse accepted = new Gson().fromJson(
                        "{\"ok\":true,\"order\":{\"id\":\"" + ORDER_ID
                                + "\",\"status\":\"done\",\"revision\":8}}",
                        OrderTransitionResponse.class
                );
                fake.pendingTransition.onSuccess(accepted);

                Button action = activity.findViewById(R.id.buttonDetailsAction);
                assertEquals(View.GONE, action.getVisibility());
                assertEquals(2, fake.attempts.size());
            });
        } finally {
            OrderDetailsGatewayFactory.setTestGateway(null);
        }
    }

    @Test
    public void orderRefreshHidesStaleDoneActionUntilBackendRevalidates() {
        FakeGateway fake = new FakeGateway(order("in_progress", 5));
        try (ActivityScenario<OrderDetailsActivity> scenario = launch(fake)) {
            scenario.onActivity(activity -> {
                Button action = activity.findViewById(R.id.buttonDetailsAction);
                assertTrue(action.isEnabled());

                fake.holdRefresh = true;
                try {
                    Method method = OrderDetailsActivity.class.getDeclaredMethod("loadOrder");
                    method.setAccessible(true);
                    method.invoke(activity);
                } catch (ReflectiveOperationException error) {
                    throw new AssertionError(error);
                }

                assertFalse(action.isEnabled());
                assertEquals(View.GONE,
                        activity.findViewById(R.id.layoutDetailsContent).getVisibility());
                assertNotNull(fake.pendingLoad);
                fake.pendingLoad.onError(0, "Synthetic refresh error");
                assertEquals(View.GONE,
                        activity.findViewById(R.id.layoutDetailsContent).getVisibility());
                assertEquals(0, fake.attempts.size());
            });
        } finally {
            OrderDetailsGatewayFactory.setTestGateway(null);
        }
    }
}
