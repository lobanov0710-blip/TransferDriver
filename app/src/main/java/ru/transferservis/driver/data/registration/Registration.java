package ru.transferservis.driver.data.registration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** Server-owned state. No identity/admission decision is inferred from these fields. */
public final class Registration {
    public static final String[] CLASSES = {"comfort", "business", "minivan"};
    public static final String[] BAGGAGE = {"reduced", "standard", "large"};
    public static final String[] USAGE = {"owner", "rental", "other"};
    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
    public final String driverId, phone, fullName, birthDate, region, status;
    public final String identityStatus;
    public final Availability availability;
    public final long revision;
    public final List<Vehicle> vehicles;

    private Registration(JsonObject profile, JsonArray rows, String expectedDriver) throws IOException {
        driverId = required(profile, "driverId", 120);
        if (!driverId.equals(expectedDriver)) throw invalid();
        phone = required(profile, "phone", 32);
        fullName = optional(profile, "fullName", 100);
        birthDate = optional(profile, "birthDate", 10);
        if (!birthDate.isEmpty()) try { LocalDate.parse(birthDate); } catch (RuntimeException e) { throw invalid(); }
        region = optional(profile, "region", 120);
        status = choice(profile, "registrationStatus", new String[]{"draft", "submitted", "under_review", "changes_required", "approved", "rejected"});
        identityStatus = optional(profile, "identityStatus", 40);
        availability = profile.has("availability") && !profile.get("availability").isJsonNull()
                ? new Availability(profile.getAsJsonObject("availability")) : null;
        revision = revision(profile);
        if (rows.size() > 200) throw invalid();
        List<Vehicle> parsed = new ArrayList<>();
        HashSet<String> ids = new HashSet<>();
        for (JsonElement row : rows) {
            if (!row.isJsonObject()) throw invalid();
            Vehicle vehicle = new Vehicle(row.getAsJsonObject(), driverId);
            if (!ids.add(vehicle.id)) throw invalid();
            parsed.add(vehicle);
        }
        vehicles = Collections.unmodifiableList(parsed);
    }
    public static Registration parse(JsonObject response, String driverId) throws IOException {
        try {
            if (!response.get("ok").isJsonPrimitive() || !response.get("ok").getAsJsonPrimitive().isBoolean()
                    || !response.get("ok").getAsBoolean()) throw invalid();
            return new Registration(response.getAsJsonObject("profile"), response.getAsJsonArray("vehicles"), driverId);
        } catch (RuntimeException malformed) { throw invalid(); }
    }
    public boolean editable() { return "draft".equals(status) || "changes_required".equals(status); }
    public static final class Availability {
        public final String state, activeVehicleId;
        public final Long revision;
        private Availability(JsonObject object) throws IOException {
            state = choice(object, "state", new String[]{"off_line", "on_line"});
            activeVehicleId = optional(object, "activeVehicleId", 120);
            if (!activeVehicleId.isEmpty() && !activeVehicleId.matches("vehicle_[A-Za-z0-9_-]{8,100}")) throw invalid();
            revision = object.has("revision") && !object.get("revision").isJsonNull() ? revision(object) : null;
            if ("on_line".equals(state) && activeVehicleId.isEmpty()) throw invalid();
        }
        public boolean online() { return "on_line".equals(state); }
    }
    public static final class Vehicle {
        public final String id, vin, plate, make, model, color, vehicleClass, baggage, usage, status;
        public final int capacity;
        public final long revision;
        private Vehicle(JsonObject row, String driver) throws IOException {
            id = required(row, "id", 80);
            if (!id.matches("vehicle_[0-9a-fA-F-]{36}") || !driver.equals(required(row, "driverId", 120))) throw invalid();
            vin = required(row, "vin", 17);
            if (!validVin(vin)) throw invalid();
            plate = required(row, "plate", 32); make = required(row, "make", 80);
            model = required(row, "model", 80); color = optional(row, "color", 80);
            vehicleClass = choice(row, "vehicleClass", CLASSES); baggage = choice(row, "baggageCapacity", BAGGAGE);
            // Existing vehicles may predate the usage-basis field; let the editor complete it.
            String basis = optional(row, "usageBasis", 40);
            if (!basis.isEmpty() && !Arrays.asList(USAGE).contains(basis)) throw invalid();
            usage = basis;
            status = choice(row, "status", new String[]{"draft", "pending_review", "approved", "photo_check_required", "suspended", "rejected"});
            long seats = number(row, "capacity");
            if (seats < 1 || seats > 20) throw invalid();
            capacity = (int) seats; revision = revision(row);
        }
        public boolean editable() { return !"suspended".equals(status) && !"rejected".equals(status); }
    }
    public static boolean validVin(String value) { return value != null && value.trim().toUpperCase(Locale.ROOT).matches("[A-HJ-NPR-Z0-9]{17}"); }
    public static String displayDate(String iso) { return iso.isEmpty() ? "" : LocalDate.parse(iso).format(DISPLAY_DATE); }
    public static String inputDate(String value) {
        try {
            LocalDate date = LocalDate.parse(value.trim(), DISPLAY_DATE);
            return date.isAfter(LocalDate.now(java.time.ZoneOffset.UTC)) ? null : date.toString();
        } catch (RuntimeException invalid) { return null; }
    }
    public static boolean textValid(String value, int min, int max) {
        return value != null && value.trim().length() >= min && value.trim().length() <= max
                && !value.matches("(?s).*[\\x00-\\x1f\\x7f].*");
    }
    private static long revision(JsonObject object) throws IOException {
        long value = number(object, "revision"); if (value < 1) throw invalid(); return value;
    }
    private static long number(JsonObject object, String key) throws IOException {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw invalid();
        try { return value.getAsBigDecimal().longValueExact(); } catch (RuntimeException e) { throw invalid(); }
    }
    private static String required(JsonObject object, String key, int max) throws IOException {
        String value = optional(object, key, max); if (value.isEmpty()) throw invalid(); return value;
    }
    private static String optional(JsonObject object, String key, int max) throws IOException {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) return "";
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw invalid();
        String text = value.getAsString().trim(); if (!textValid(text, 0, max)) throw invalid(); return text;
    }
    private static String choice(JsonObject object, String key, String[] choices) throws IOException {
        String value = required(object, key, 40); if (!Arrays.asList(choices).contains(value)) throw invalid(); return value;
    }
    private static IOException invalid() { return new IOException("Invalid registration response"); }
}
