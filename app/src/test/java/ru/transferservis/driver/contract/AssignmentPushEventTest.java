package ru.transferservis.driver.contract;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import ru.transferservis.driver.notifications.AssignmentPushEvent;
import static org.junit.Assert.*;

public class AssignmentPushEventTest {
    private Map<String, String> assigned() {
        Map<String, String> data = new HashMap<>();
        data.put("type", "order_assigned");
        data.put("orderId", "order_12345678");
        data.put("assignmentId", "assignment_12345678");
        data.put("orderRevision", "2");
        return data;
    }

    @Test public void acceptsCurrentBackendAssignmentAndRevocation() {
        Map<String, String> data = assigned();
        assertFalse(AssignmentPushEvent.parse(data).revoked);
        data.put("type", "order_assignment_revoked");
        data.remove("assignmentId");
        assertTrue(AssignmentPushEvent.parse(data).revoked);
        data.put("orderId", "123e4567-e89b-12d3-a456-426614174000");
        assertNotNull(AssignmentPushEvent.parse(data));
    }

    @Test public void rejectsLegacyBroadcastAndMalformedHints() {
        assertNull(AssignmentPushEvent.parse(null));
        for (String type : new String[]{"new_order", "unknown", ""}) {
            Map<String, String> data = assigned(); data.put("type", type);
            assertNull(AssignmentPushEvent.parse(data));
        }
        for (String id : new String[]{"", "../order", "order\nsecret", "https://example.com"}) {
            Map<String, String> data = assigned(); data.put("orderId", id);
            assertNull(AssignmentPushEvent.parse(data));
        }
        for (String revision : new String[]{"0", "-1", "1.5", "9007199254740992", "999999999999999999999"}) {
            Map<String, String> data = assigned(); data.put("orderRevision", revision);
            assertNull(AssignmentPushEvent.parse(data));
        }
        Map<String, String> data = assigned(); data.remove("assignmentId");
        assertNull(AssignmentPushEvent.parse(data));
    }
}
