package ru.transferservis.driver.data.registration;
import com.google.gson.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class AvailabilityModelTest {
    private JsonObject data(){return JsonParser.parseString("{\"ok\":true,\"profile\":{\"driverId\":\"synthetic-driver\",\"phone\":\"+70000000000\",\"fullName\":null,\"region\":null,\"birthDate\":null,\"registrationStatus\":\"approved\",\"identityStatus\":\"verified\",\"revision\":3,\"availability\":{\"state\":\"off_line\",\"activeVehicleId\":null,\"revision\":7}},\"vehicles\":[]}").getAsJsonObject();}
    private void reject(JsonObject value){try{Registration.parse(value,"synthetic-driver");fail();}catch(java.io.IOException expected){}}
    @Test public void availabilityHasIndependentRevisionAndNoImplicitVehicle() throws Exception {
        Registration r=Registration.parse(data(),"synthetic-driver");assertEquals(3,r.revision);assertEquals(Long.valueOf(7),r.availability.revision);assertFalse(r.availability.online());assertEquals("",r.availability.activeVehicleId);assertEquals("verified",r.identityStatus);
    }
    @Test public void missingAvailabilityCannotBeAssumedOfflineReady() throws Exception {
        JsonObject d=data();d.getAsJsonObject("profile").remove("availability");assertNull(Registration.parse(d,"synthetic-driver").availability);
        d=data();d.getAsJsonObject("profile").getAsJsonObject("availability").add("revision",JsonNull.INSTANCE);assertNull(Registration.parse(d,"synthetic-driver").availability.revision);
    }
    @Test public void onlineRequiresServerSelectedVehicle() {
        JsonObject d=data();d.getAsJsonObject("profile").getAsJsonObject("availability").addProperty("state","on_line");reject(d);
    }
    @Test public void unknownStatesFractionalRevisionsAndUnsafeVehicleIdsFailClosed() {
        JsonObject d=data();d.getAsJsonObject("profile").getAsJsonObject("availability").addProperty("state","ready");reject(d);
        d=data();d.getAsJsonObject("profile").getAsJsonObject("availability").addProperty("revision",1.5);reject(d);
        d=data();d.getAsJsonObject("profile").getAsJsonObject("availability").addProperty("activeVehicleId","../another-driver");reject(d);
    }
    @Test public void onlineStateIsReadFromServerNotIdentityGuess() throws Exception {
        JsonObject d=data();JsonObject a=d.getAsJsonObject("profile").getAsJsonObject("availability");a.addProperty("state","on_line");a.addProperty("activeVehicleId","vehicle_00000000-0000-4000-8000-000000000001");
        assertTrue(Registration.parse(d,"synthetic-driver").availability.online());
    }
}
