package ru.transferservis.driver.notifications;

import java.util.Map;

/** Push is a routing hint; the order screen always reloads authoritative data. */
public final class AssignmentPushEvent {
    public final String orderId;
    public final boolean revoked;

    private AssignmentPushEvent(String orderId, boolean revoked) {
        this.orderId = orderId;
        this.revoked = revoked;
    }

    public static AssignmentPushEvent parse(Map<String, String> data) {
        if (data == null) return null;
        String type = data.get("type");
        boolean revoked = "order_assignment_revoked".equals(type);
        if (!revoked && !"order_assigned".equals(type)) return null;
        String orderId = data.get("orderId");
        if (!isOpaqueId(orderId)) return null;
        if (!revoked && !isOpaqueId(data.get("assignmentId"))) return null;
        String revision = data.get("orderRevision");
        if (revision == null || !revision.matches("[1-9][0-9]{0,15}")) return null;
        if (Long.parseLong(revision) > 9_007_199_254_740_991L) return null;
        return new AssignmentPushEvent(orderId, revoked);
    }

    private static boolean isOpaqueId(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{1,120}");
    }
}
