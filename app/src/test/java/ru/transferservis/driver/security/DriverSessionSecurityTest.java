package ru.transferservis.driver.security;

import org.junit.Test;
import ru.transferservis.driver.data.session.DriverSession;
import static org.junit.Assert.*;

public class DriverSessionSecurityTest {
    private DriverSession session(String role, String access, long accessExpiry,
                                  String refresh, long refreshExpiry) {
        return new DriverSession("test-driver", "Test", role, access,
                accessExpiry, refresh, refreshExpiry);
    }

    @Test public void rejectsExpiredAndBlankTokens() {
        assertFalse(session("driver", "test", 1, "test", 1).hasUsableAccessToken());
        assertFalse(session("driver", "test", 1, "test", 1).hasUsableRefreshToken());
        assertFalse(session("driver", " ", Long.MAX_VALUE, null, Long.MAX_VALUE).hasUsableAccessToken());
        assertFalse(session("driver", "test", 1, " ", Long.MAX_VALUE).hasUsableRefreshToken());
    }

    @Test public void accessSafetyWindowDoesNotDiscardRefresh() {
        DriverSession session = session("driver", "test-access", System.currentTimeMillis() + 10_000,
                "test-refresh", Long.MAX_VALUE);
        assertFalse(session.hasUsableAccessToken());
        assertTrue(session.hasUsableRefreshToken());
    }

    @Test public void adminRoleDoesNotBecomeDriver() {
        assertFalse(session("admin", "test", 1, "test", 1).isDriver());
        assertFalse(session("owner", "test", 1, "test", 1).isDriver());
        assertFalse(session(null, "test", 1, "test", 1).isDriver());
        assertTrue(session(" DRIVER ", "test", 1, "test", 1).isDriver());
    }
}
