package ru.transferservis.admin.data;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;
public final class RegistrationReviewTest {
    @Test public void revisionTargetAndHistoryAreBoundToTheRequestedDriver() throws Exception {
        JsonObject body=ModerationTestData.body();JsonObject history=new JsonObject();history.addProperty("targetType","document");history.addProperty("targetId","document_fixture001");history.addProperty("decision","changes_required");history.addProperty("note","Synthetic reason");history.addProperty("createdAt",1800000000000L);
        body.getAsJsonObject("registration").getAsJsonArray("reviews").add(history);
        RegistrationReview review=RegistrationReview.parse(body,"staff_driver123");assertEquals(2,review.revision);assertEquals("Synthetic reason",review.history.get(0).note);assertTrue(review.items.get(0).pending());
        history.addProperty("targetId","document_foreign123");try{RegistrationReview.parse(body,"staff_driver123");fail();}catch(java.io.IOException expected){}
    }
    @Test public void malformedRevisionsCrossDriverAndUnknownFieldsCannotAuthorizeOrLeakObjectKeys() throws Exception {
        JsonObject body=ModerationTestData.body();body.getAsJsonObject("registration").addProperty("privateObjectKey","forbidden-private-key");
        assertFalse(new Gson().toJson(RegistrationReview.parse(body,"staff_driver123")).contains("forbidden-private-key"));
        for(String value:new String[]{"staff_otherdriver123","../staff_driver123","https://fixture.invalid"})try{RegistrationReview.parse(body,value);fail();}catch(java.io.IOException expected){}
        for(String revision:new String[]{"0","1.5","\"2\""}){
            JsonObject invalid=ModerationTestData.body();invalid.getAsJsonObject("registration").getAsJsonObject("profile").add("revision",com.google.gson.JsonParser.parseString(revision));
            try{RegistrationReview.parse(invalid,"staff_driver123");fail();}catch(java.io.IOException expected){}
        }
    }
    @Test public void duplicateAndForeignVehicleResourcesFailClosed() throws Exception {
        JsonObject body=ModerationTestData.body(),registration=body.getAsJsonObject("registration");
        registration.getAsJsonArray("documents").add(registration.getAsJsonArray("documents").get(0).deepCopy());
        try{RegistrationReview.parse(body,"staff_driver123");fail();}catch(java.io.IOException expected){}
        body=ModerationTestData.body();body.getAsJsonObject("registration").getAsJsonArray("documents").get(0).getAsJsonObject().addProperty("vehicleId","vehicle_foreign123");
        try{RegistrationReview.parse(body,"staff_driver123");fail();}catch(java.io.IOException expected){}
    }
    @Test public void permissionSetsDoNotConflateModerationSensitiveReadAndFinalApproval() throws Exception {
        RegistrationReview.Item document=ModerationTestData.review().items.get(0);
        AdminAccess read=ModerationTestData.access("[\"drivers.read\"]");assertFalse(read.canReadReview());assertFalse(read.canReviewItem(document));
        AdminAccess reviewer=ModerationTestData.access("[\"drivers.verify_documents\"]");assertTrue(reviewer.canReviewItem(document));assertFalse(reviewer.canReadResource(document));assertFalse(reviewer.canFinalizeReview());
        assertTrue(ModerationTestData.access("[\"drivers.verify_documents\",\"documents.read_sensitive\"]").canReadResource(document));
        assertFalse(ModerationTestData.access("[\"documents.read_sensitive\"]").canReadReview());
        assertFalse(ModerationTestData.access("[\"*\"]").canStartReview());
    }
    @Test public void negativeDecisionRequiresReasonAndExplicitRetryKeepsBodyRevisionAndKey() throws Exception {
        RegistrationReview review=ModerationTestData.review();AdminAccess access=ModerationTestData.access("[\"drivers.verify_documents\"]");
        for(String reason:new String[]{"", " ", "invalid\u0001reason"})try{ReviewAction.decide(access,review,review.items.get(0),"changes_required",reason);fail();}catch(java.io.IOException expected){}
        ReviewAction action=ReviewAction.decide(access,review,review.items.get(0),"changes_required","Synthetic reason");
        String key=action.key,body=action.body().toString();action.body().addProperty("decision","verified");
        assertEquals(key,action.key);assertEquals(body,action.body().toString());assertEquals(1,action.revision);
        try{ReviewAction.decide(access,review,null,"approved","");fail();}catch(java.io.IOException expected){}
    }
}
