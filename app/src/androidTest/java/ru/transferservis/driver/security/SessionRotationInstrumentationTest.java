package ru.transferservis.driver.security;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import ru.transferservis.driver.data.model.StaffLoginResponse;
import ru.transferservis.driver.data.session.DriverSession;
import ru.transferservis.driver.data.session.SecureSessionStore;
import ru.transferservis.driver.data.session.StaffSessionRefresher;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class SessionRotationInstrumentationTest {
    private SecureSessionStore store;
    private SecureSessionStore otherStore;

    @Before public void setup() {
        store = new SecureSessionStore(InstrumentationRegistry.getInstrumentation().getTargetContext());
        otherStore = new SecureSessionStore(InstrumentationRegistry.getInstrumentation().getTargetContext());
        store.clear();
    }

    @After public void cleanup() { store.clear(); }

    private StaffLoginResponse response(String driverId, String token) {
        long expiry = System.currentTimeMillis() + 3_600_000L;
        return new Gson().fromJson("{\"ok\":true,\"staff\":{\"id\":\"" + driverId
                + "\",\"displayName\":\"Test Driver\",\"role\":\"driver\"},"
                + "\"accessToken\":\"access-" + token + "\",\"accessExpiresAt\":" + expiry
                + ",\"refreshToken\":\"refresh-" + token + "\",\"refreshExpiresAt\":" + expiry + "}",
                StaffLoginResponse.class);
    }

    @Test public void delayedRotationCannotRestoreSessionAfterLogout() throws Exception {
        assertTrue(store.save(response("driver-test", "old")));
        DriverSession original = store.load();
        CountDownLatch responseArrives = new CountDownLatch(1);
        FutureTask<DriverSession> rotation = new FutureTask<>(() -> {
            assertTrue(responseArrives.await(5, TimeUnit.SECONDS));
            return otherStore.rotateIfCurrent(original, response("driver-test", "rotated"));
        });
        Thread worker = new Thread(rotation);
        worker.start();
        try {
            store.clear();
            responseArrives.countDown();
            assertNull(rotation.get(5, TimeUnit.SECONDS));
            assertNull(store.load());
        } finally {
            responseArrives.countDown();
            worker.join(5000);
        }
    }

    @Test public void staleSuccessAndFailureCannotOverwriteANewerLogin() {
        for (String nextId : new String[]{"driver-test", "other-driver"}) {
            assertTrue(store.save(response("driver-test", "old")));
            DriverSession original = store.load();
            assertTrue(otherStore.save(response(nextId, "new-login")));
            assertNull(store.rotateIfCurrent(original, response("driver-test", "stale")));
            store.clearIfCurrent(original);
            assertEquals(nextId, otherStore.load().getStaffId());
            assertEquals("refresh-new-login", otherStore.load().getRefreshToken());
        }
    }

    @Test public void rotationIsVisibleAcrossInstancesAndOldTokenCannotBeReused() {
        assertTrue(store.save(response("driver-test", "old")));
        DriverSession original = otherStore.load();
        assertNotNull(store.rotateIfCurrent(original, response("driver-test", "rotated")));
        assertEquals("refresh-rotated", otherStore.load().getRefreshToken());
        assertNull(otherStore.rotateIfCurrent(original, response("driver-test", "replay")));
        assertEquals("refresh-rotated", store.load().getRefreshToken());
    }

    @Test public void unauthorizedRequestCannotRetryAsAnotherDriver() {
        assertTrue(store.save(response("other-driver", "new-login")));
        StaffSessionRefresher.Result result = new StaffSessionRefresher(otherStore)
                .refreshAfterUnauthorizedBlocking("access-old", "driver-test");
        assertEquals(StaffSessionRefresher.ResultType.AUTHENTICATION_REQUIRED, result.getType());
        assertEquals("other-driver", store.load().getStaffId());
        assertEquals("refresh-new-login", store.load().getRefreshToken());
    }
}
