package ru.transferservis.admin.data;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import static org.junit.Assert.*;
public final class RegistrationRowTest {
    @Test public void queueIsReadWithoutInventingDraftOrApprovalFilters() throws Exception {
        for (String status:new String[]{"submitted","under_review","changes_required"}) {
            JsonObject body=JsonParser.parseString("{\"ok\":true,\"registrations\":[{\"driverId\":\"driver-1\",\"registrationStatus\":\""+status+"\",\"fullName\":\"Test Driver\"}]}").getAsJsonObject();
            assertEquals(status,RegistrationRow.parse(body).get(0).status);
        }
    }
    @Test public void malformedQueueAndUnexpectedStatusesRejected() {
        for (String json:new String[]{"{}","{\"ok\":true,\"registrations\":[{}]}",
                "{\"ok\":true,\"registrations\":[{\"driverId\":\"d\",\"registrationStatus\":\"approved\"}]}"}) {
            try { RegistrationRow.parse(JsonParser.parseString(json).getAsJsonObject());fail("Must reject"); }
            catch(java.io.IOException expected) { assertFalse(expected.getMessage().contains("driverId")); }
        }
    }
}
