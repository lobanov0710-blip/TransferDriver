package ru.transferservis.admin.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RegistrationRow {
    public final String fullName, region, status;
    private RegistrationRow(String name, String region, String status) {
        fullName = name; this.region = region; this.status = status;
    }
    public static List<RegistrationRow> parse(JsonObject response) throws IOException {
        try {
            if (!response.getAsJsonPrimitive("ok").isBoolean() || !response.get("ok").getAsBoolean()
                    || !response.get("registrations").isJsonArray()
                    || response.getAsJsonArray("registrations").size() > 50) throw new IOException("Invalid queue");
            List<RegistrationRow> rows = new ArrayList<>();
            for (JsonElement item : response.getAsJsonArray("registrations")) {
                JsonObject row = item.getAsJsonObject();
                String id = AdminSession.string(row, "driverId"), status = AdminSession.string(row, "registrationStatus");
                if (id.isEmpty() || !("submitted".equals(status) || "under_review".equals(status)
                        || "changes_required".equals(status))) throw new IOException("Invalid registration");
                rows.add(new RegistrationRow(AdminSession.string(row, "fullName"), AdminSession.string(row, "region"), status));
            }
            return Collections.unmodifiableList(rows);
        } catch (RuntimeException invalid) { throw new IOException("Invalid queue"); }
    }
}
