package ru.transferservis.driver.data.registration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.Test;
import static org.junit.Assert.*;

public final class RegistrationTest {
    private JsonObject draft() {
        return JsonParser.parseString("{\"ok\":true,\"profile\":{\"driverId\":\"synthetic-driver\",\"phone\":\"+70000000000\",\"fullName\":null,\"birthDate\":null,\"region\":null,\"registrationStatus\":\"draft\",\"revision\":1},\"vehicles\":[]}").getAsJsonObject();
    }
    private void rejects(JsonObject value) {
        try { Registration.parse(value,"synthetic-driver"); fail("Unsafe response accepted"); } catch(IOException expected) { }
    }
    @Test public void emptyDraftAndPhoneAreServerOwned() throws Exception {
        Registration value=Registration.parse(draft(),"synthetic-driver");
        assertTrue(value.editable()); assertEquals("+70000000000",value.phone);assertTrue(value.vehicles.isEmpty());assertEquals("",value.fullName);
    }
    @Test public void foreignProfileCannotReplaceCurrentDriver() {
        JsonObject value=draft();value.getAsJsonObject("profile").addProperty("driverId","another-driver");rejects(value);
    }
    @Test public void onlyDraftAndChangesRequiredPermitProfileEditing() throws Exception {
        for(String status:new String[]{"draft","submitted","under_review","changes_required","approved","rejected"}) {
            JsonObject value=draft();value.getAsJsonObject("profile").addProperty("registrationStatus",status);
            assertEquals(status.equals("draft")||status.equals("changes_required"),Registration.parse(value,"synthetic-driver").editable());
        }
    }
    @Test public void invalidRevisionOrStatusFailsClosed() {
        for(String json:new String[]{"0","-1","1.5","\"1\"","null"}) {
            JsonObject value=draft();value.getAsJsonObject("profile").add("revision",JsonParser.parseString(json));rejects(value);
        }
        JsonObject value=draft();value.getAsJsonObject("profile").addProperty("registrationStatus","verified");rejects(value);
    }
    @Test public void malformedOkCannotBecomeSuccess() {
        for(String ok:new String[]{"false","1","\"true\"","null"}) { JsonObject value=draft();value.add("ok",JsonParser.parseString(ok));rejects(value); }
    }
    @Test public void vinExcludesAmbiguousLettersAndNonLatinCharacters() {
        assertTrue(Registration.validVin(" xw8zzz61zfg000001 "));
        for(String vin:new String[]{"","XW8ZZZ61ZFG00000I","XW8ZZZ61ZFG00000O","XW8ZZZ61ZFG00000Q","ХW8ZZZ61ZFG000001","XW8ZZZ61ZFG0000011"})assertFalse(Registration.validVin(vin));
    }
    @Test public void dateInputUsesStrictCalendarAndServerFormat() {
        assertEquals("2000-02-29",Registration.inputDate("29.02.2000"));assertEquals("29.02.2000",Registration.displayDate("2000-02-29"));
        for(String date:new String[]{"29.02.2001","31.04.2000","2000-02-29","01.13.2000",LocalDate.now(ZoneOffset.UTC).plusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.uuuu"))})assertNull(Registration.inputDate(date));
    }
    @Test public void controlCharactersAndOversizedFieldsRejected() {
        assertFalse(Registration.textValid("Иван\nТест",2,100)); assertFalse(Registration.textValid("x".repeat(121),1,120));
        JsonObject value=draft();value.getAsJsonObject("profile").addProperty("fullName","test\u0000name");rejects(value);
    }
    private JsonObject vehicle() {
        return JsonParser.parseString("{\"id\":\"vehicle_00000000-0000-4000-8000-000000000001\",\"driverId\":\"synthetic-driver\",\"vin\":\"XW8ZZZ61ZFG000001\",\"plate\":\"TEST-001\",\"make\":\"Synthetic\",\"model\":\"Vehicle\",\"color\":null,\"vehicleClass\":\"comfort\",\"capacity\":4,\"baggageCapacity\":\"standard\",\"usageBasis\":\"owner\",\"status\":\"draft\",\"revision\":1}").getAsJsonObject();
    }
    @Test public void vehiclesRetainIndependentStatusesAndRevisions() throws Exception {
        JsonObject value=draft();value.getAsJsonArray("vehicles").add(vehicle());JsonObject second=vehicle();
        second.addProperty("id","vehicle_00000000-0000-4000-8000-000000000002");second.addProperty("status","suspended");second.addProperty("revision",7);value.getAsJsonArray("vehicles").add(second);
        Registration parsed=Registration.parse(value,"synthetic-driver");assertTrue(parsed.vehicles.get(0).editable());assertFalse(parsed.vehicles.get(1).editable());assertEquals(7,parsed.vehicles.get(1).revision);
    }
    @Test public void foreignDuplicateAndMalformedVehiclesAreRejected() {
        JsonObject value=draft();JsonObject vehicle=vehicle();value.getAsJsonArray("vehicles").add(vehicle);vehicle.addProperty("driverId","another-driver");rejects(value);
        value=draft();value.getAsJsonArray("vehicles").add(vehicle());value.getAsJsonArray("vehicles").add(vehicle());rejects(value);
        value=draft();vehicle=vehicle();vehicle.addProperty("capacity",2.5);value.getAsJsonArray("vehicles").add(vehicle);rejects(value);
    }
    @Test public void olderVehicleWithoutUsageBasisCanBeCompleted() throws Exception {
        JsonObject value=draft();JsonObject vehicle=vehicle();vehicle.add("usageBasis",com.google.gson.JsonNull.INSTANCE);value.getAsJsonArray("vehicles").add(vehicle);
        assertEquals("",Registration.parse(value,"synthetic-driver").vehicles.get(0).usage);
    }
}
