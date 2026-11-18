package ru.transferservis.admin.data;
import com.google.gson.*;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
public final class VehicleClassificationTest {
    @Test public void verifiedLiftbackIsPreservedForCandidateAndExcludedMetadata()throws Exception {
        JsonObject value=current();value.getAsJsonObject("verifiedParameters").addProperty("bodyType","liftback");
        VehicleClassification.Page parsed=VehicleClassification.Page.parse(value,CAR,"",4L);
        assertEquals("liftback",parsed.verified.bodyType);assertEquals("comfort",parsed.decision.vehicleClass);
        value.add("classification",json("{\"outcome\":\"excluded\",\"vehicleClass\":null,\"ruleVersion\":\"rule_fixture001\",\"reasonCodes\":[\"body_type_not_allowed\"]}"));
        parsed=VehicleClassification.Page.parse(value,CAR,"",4L);assertEquals("liftback",parsed.verified.bodyType);assertNull(parsed.decision.vehicleClass);
    }
    static final String CAR="vehicle_fixture001";
    static JsonObject json(String text){return JsonParser.parseString(text).getAsJsonObject();}
    static JsonObject metadata(){return json("{\"ok\":true,\"requestId\":\"req_fixture001\",\"vehicleId\":\""+CAR+"\",\"vehicleRevision\":4,\"inputRevision\":1,\"declaredParameters\":{\"make\":\"Toyota\",\"model\":\"Camry\",\"modelYear\":2023,\"color\":null},\"verifiedParameters\":null,\"pinnedRuleVersion\":\"rule_fixture001\",\"classification\":null,\"evidence\":[],\"conditions\":[],\"history\":[],\"nextHistoryCursor\":null}");}
    static JsonObject current(){JsonObject b=metadata();b.add("verifiedParameters",json("{\"make\":\"Toyota\",\"model\":\"Camry\",\"modelYear\":2023,\"powertrain\":\"hybrid\",\"bodyType\":\"sedan\"}"));b.add("classification",json("{\"outcome\":\"candidate\",\"vehicleClass\":\"comfort\",\"ruleVersion\":\"rule_fixture001\",\"reasonCodes\":[]}"));for(String id:new String[]{"document_front001","document_back0001"})b.getAsJsonArray("evidence").add(json("{\"documentId\":\""+id+"\",\"expectedRevision\":1}"));return b;}
    static JsonObject record(String id){return json("{\"assessmentId\":\""+id+"\",\"vehicleRevision\":2,\"ruleVersion\":null,\"outcome\":\"candidate\",\"createdAt\":1800000000000}");}
    private interface Invalid{void run()throws Exception;}
    private void rejects(Invalid action)throws Exception {try{action.run();fail("Invalid classification accepted");}catch(IOException expected){}}
    @Test public void nullYearAndClassStayUnknownDespiteHistoricalCandidate()throws Exception {
        JsonObject b=metadata();b.getAsJsonObject("declaredParameters").add("modelYear",JsonNull.INSTANCE);b.getAsJsonArray("history").add(record("assessment_old001"));
        VehicleClassification.Page p=VehicleClassification.Page.parse(b,CAR,"",null);assertNull(p.verified);assertNull(p.decision);assertNull(p.declared.year);assertNull(p.declared.color);assertEquals("candidate",p.history.get(0).outcome);
    }
    @Test public void validCurrentProjectionIsImmutableAndSeparatesDeclaredFromVerified()throws Exception {
        VehicleClassification.Page p=VehicleClassification.Page.parse(current(),CAR,"",4L);assertEquals("hybrid",p.verified.powertrain);assertEquals("comfort",p.decision.vehicleClass);assertEquals(2,p.evidence.size());
        try{p.evidence.clear();fail();}catch(UnsupportedOperationException expected){}
    }
    @Test public void privateUnknownAndMissingFieldsFailClosed()throws Exception {
        for(String key:new String[]{"vin","phone","reason","privateObjectKey","url","approved"}){JsonObject b=current();b.addProperty(key,"PRIVATE");rejects(()->VehicleClassification.Page.parse(b,CAR,"",null));}
        JsonObject b=current();b.getAsJsonObject("verifiedParameters").addProperty("yearVerified",true);rejects(()->VehicleClassification.Page.parse(b,CAR,"",null));
        JsonObject missing=metadata();missing.remove("verifiedParameters");rejects(()->VehicleClassification.Page.parse(missing,CAR,"",null));
    }
    @Test public void vehicleAndGenerationCannotCrossPages()throws Exception {
        rejects(()->VehicleClassification.Page.parse(current(),"vehicle_other0001","",null));
        try{VehicleClassification.Page.parse(current(),CAR,"",5L);fail();}catch(AdminApiClient.Failure e){assertEquals(409,e.status);}
    }
    @Test public void forgedEvidenceOrMismatchedVerificationIsRejected()throws Exception {
        JsonObject b=current();b.getAsJsonObject("verifiedParameters").addProperty("modelYear",2024);rejects(()->VehicleClassification.Page.parse(b,CAR,"",null));
        JsonObject duplicate=current();duplicate.getAsJsonArray("evidence").add(duplicate.getAsJsonArray("evidence").get(0).deepCopy());rejects(()->VehicleClassification.Page.parse(duplicate,CAR,"",null));
        JsonObject absent=current();absent.add("verifiedParameters",JsonNull.INSTANCE);rejects(()->VehicleClassification.Page.parse(absent,CAR,"",null));
    }
    @Test public void classOutcomesAndScalarTypesAreStrict()throws Exception {
        for(String value:new String[]{"0","10000","2023.5","\"2023\"","true"}){JsonObject b=metadata();b.getAsJsonObject("declaredParameters").add("modelYear",JsonParser.parseString(value));rejects(()->VehicleClassification.Page.parse(b,CAR,"",null));}
        JsonObject b=current();b.getAsJsonObject("classification").addProperty("outcome","excluded");rejects(()->VehicleClassification.Page.parse(b,CAR,"",null));
        JsonObject economy=current();economy.getAsJsonObject("classification").addProperty("vehicleClass","economy");rejects(()->VehicleClassification.Page.parse(economy,CAR,"",null));
    }
    @Test public void duplicateHistoryAndOversizeResponsesAreRejected()throws Exception {
        JsonObject b=metadata();b.getAsJsonArray("history").add(record("assessment_old001"));b.getAsJsonArray("history").add(record("assessment_old001"));rejects(()->VehicleClassification.Page.parse(b,CAR,"",null));
        JsonObject large=metadata();for(int n=0;n<51;n++)large.getAsJsonArray("history").add(record("assessment_"+n));rejects(()->VehicleClassification.Page.parse(large,CAR,"",null));
    }
    @Test public void cursorMustAdvanceWithoutPathOrQueryInjection()throws Exception {
        for(String value:new String[]{"../PRIVATE","cursor&vehicle=other","x".repeat(1025)})rejects(()->VehicleClassification.cursor(value));
        for(String value:new String[]{".","..","../rule","rule?query=secret"})rejects(()->VehicleClassification.version(value));
        JsonObject b=metadata();b.addProperty("nextHistoryCursor","cursor_page001");rejects(()->VehicleClassification.Page.parse(b,CAR,"cursor_page001",null));
    }
    @Test public void onlyExistingVerifyPermissionOpensCardAndPhoneScopeStaysClosed()throws Exception {
        assertTrue(AssignmentTestData.access("vehicles.verify").canReadClassification());assertFalse(AssignmentTestData.access("vehicles.read","vehicles.approve_exception").canReadClassification());
        JsonObject a=json("{\"ok\":true,\"admin\":{\"accountId\":\"staff_fixture001\",\"role\":\"owner\",\"permissions\":[\"*\"],\"testScope\":\"owner_phone\",\"mfa\":{\"required\":true,\"verified\":true,\"method\":\"totp\",\"expiresAt\":9007199254740991}}}");
        assertFalse(AdminAccess.fromResponse(a,"staff_fixture001").canReadClassification());
    }
    @Test public void basisVersionDoesNotReplaceTheActualUnknownModel()throws Exception {
        JsonObject b=current();b.add("pinnedRuleVersion",JsonNull.INSTANCE);b.getAsJsonObject("declaredParameters").addProperty("model","Unknown");b.getAsJsonObject("verifiedParameters").addProperty("model","Unknown");
        VehicleClassification.Page p=VehicleClassification.Page.parse(b,CAR,"",null);assertEquals("Unknown",p.declared.model);assertEquals("Unknown",p.verified.model);assertEquals("rule_fixture001",p.decision.ruleVersion);
    }
}
