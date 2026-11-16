package ru.transferservis.admin.data;
import com.google.gson.*;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
public final class AssignmentContractTest {
    private interface Check{void run()throws IOException;}
    private void rejects(Check check){try{check.run();fail();}catch(IOException expected){assertFalse(expected.getMessage().contains("PRIVATE"));}}
    private void rejectOrders(JsonObject body){rejects(()->Assignments.Orders.parse(body,""));}
    private void rejectOptions(JsonObject body){rejects(()->Assignments.Options.parse(body,AssignmentTestData.parsedOrder(),""));}
    @Test public void minimalNullableReadKeepsIndependentOrderRevision()throws Exception {
        Assignments.Order order=AssignmentTestData.parsedOrder();assertNull(order.to);assertNull(order.assignment);assertTrue(order.newAssignment());assertEquals(7,order.revision);
        Assignments.Option option=AssignmentTestData.parsedOptions().rows.get(0);assertNull(option.fullName);assertTrue(option.eligible);assertEquals("off_line",option.state);
    }
    @Test public void contactFieldsUnexpectedStateAndInvalidCalendarFailClosed(){
        JsonObject b=AssignmentTestData.orders();b.getAsJsonArray("orders").get(0).getAsJsonObject().addProperty("phone","PRIVATE");rejectOrders(b);
        b=AssignmentTestData.orders();b.getAsJsonArray("orders").get(0).getAsJsonObject().addProperty("status","ready");rejectOrders(b);
        b=AssignmentTestData.orders();b.getAsJsonArray("orders").get(0).getAsJsonObject().addProperty("date","2030-02-30");rejectOrders(b);
    }
    @Test public void fractionalUnsafeRevisionAndDuplicateOrOversizedPagesFailClosed(){
        for(double revision:new double[]{1.5,0,9_007_199_254_740_992d}){JsonObject b=AssignmentTestData.orders();b.getAsJsonArray("orders").get(0).getAsJsonObject().addProperty("revision",revision);rejectOrders(b);}
        JsonObject b=AssignmentTestData.orders();b.getAsJsonArray("orders").add(AssignmentTestData.order());rejectOrders(b);
        b=AssignmentTestData.orders();for(int i=0;i<50;i++)b.getAsJsonArray("orders").add(AssignmentTestData.order());rejectOrders(b);
    }
    @Test public void cursorRejectsUnsafeRepeatedAndEmptyContinuation(){
        JsonObject b=AssignmentTestData.orders();b.addProperty("nextCursor","../PRIVATE");rejectOrders(b);
        JsonObject repeated=AssignmentTestData.orders();repeated.addProperty("nextCursor","same_cursor");rejects(()->Assignments.Orders.parse(repeated,"same_cursor"));
        b=AssignmentTestData.orders();b.getAsJsonArray("orders").remove(0);b.addProperty("nextCursor","cursor");rejectOrders(b);
    }
    @Test public void optionsAreBoundToOriginalOrderAndRevision()throws Exception {
        JsonObject b=AssignmentTestData.options();b.addProperty("orderId","order_other001");rejectOptions(b);
        b=AssignmentTestData.options();b.addProperty("orderRevision",8);try{Assignments.Options.parse(b,AssignmentTestData.parsedOrder(),"");fail();}catch(AdminApiClient.Failure changed){assertEquals(409,changed.status);}
    }
    @Test public void eligibilityCannotContradictBlockersOrOnlineVehicle(){
        JsonObject b=AssignmentTestData.options();b.getAsJsonArray("options").get(0).getAsJsonObject().getAsJsonArray("blockers").add("vehicle_not_approved");rejectOptions(b);
        b=AssignmentTestData.options();b.getAsJsonArray("options").get(0).getAsJsonObject().getAsJsonObject("availability").addProperty("state","on_line");rejectOptions(b);
        b=AssignmentTestData.options();b.getAsJsonArray("options").get(0).getAsJsonObject().addProperty("status","suspended");rejectOptions(b);
    }
    @Test public void blockedFutureCodeIsSafeButDuplicatePairIsRejected()throws Exception {
        JsonObject b=AssignmentTestData.options(),row=b.getAsJsonArray("options").get(0).getAsJsonObject();row.addProperty("eligible",false);row.getAsJsonArray("blockers").add("future_precondition");
        assertFalse(Assignments.Options.parse(b,AssignmentTestData.parsedOrder(),"").rows.get(0).eligible);
        b.getAsJsonArray("options").add(row.deepCopy());rejectOptions(b);
    }
    @Test public void reasonIsMandatoryBoundedAndRetryBodyAndKeyAreImmutable()throws Exception {
        Assignments.Options page=AssignmentTestData.parsedOptions();Assignments.Option option=page.rows.get(0);
        for(String note:new String[]{"","\u00a0\uFEFF","x\u0001","x".repeat(501)})rejects(()->AssignmentAction.create(AssignmentTestData.all(),3,page,option,note));
        AssignmentAction action=AssignmentAction.create(AssignmentTestData.all(),3,page,option,"\u00a0 Test reason \u00a0");assertEquals("Test reason",action.reason);
        assertEquals(4,action.body().size());assertEquals(7,action.body().get("expectedRevision").getAsLong());assertEquals(action.body(),action.body());assertEquals(3,action.scope);assertTrue(action.key.startsWith("assignment-"));
    }
    @Test public void changedOptionSnapshotAndMissingActionGrantCannotAuthorize()throws Exception {
        Assignments.Options first=AssignmentTestData.parsedOptions(),second=AssignmentTestData.parsedOptions();
        rejects(()->AssignmentAction.create(AssignmentTestData.all(),1,first,second.rows.get(0),"Reason"));
        rejects(()->AssignmentAction.create(AssignmentTestData.access("orders.read","orders.assign"),1,first,first.rows.get(0),"Reason"));
    }
    @Test public void reassignUsesOrderRevisionAndRequiresReassignGrant()throws Exception {
        JsonObject b=AssignmentTestData.orders(),row=b.getAsJsonArray("orders").get(0).getAsJsonObject();row.addProperty("status","taken");
        row.add("assignment",JsonParser.parseString("{\"id\":\"assignment_original001\",\"driverId\":\"staff_driver002\",\"vehicleId\":\"vehicle_fixture002\",\"revision\":2,\"phase\":\"assigned\"}"));
        Assignments.Order order=Assignments.Orders.parse(b,"").rows.get(0);Assignments.Options page=Assignments.Options.parse(AssignmentTestData.options(),order,"");
        AssignmentAction action=AssignmentAction.create(AssignmentTestData.all(),1,page,page.rows.get(0),"Reason");assertTrue(action.reassign);assertEquals(7,action.body().get("expectedRevision").getAsLong());
        rejects(()->AssignmentAction.create(AssignmentTestData.access("orders.read","drivers.read","vehicles.read","orders.assign"),1,page,page.rows.get(0),"Reason"));
    }
    @Test public void inProgressCannotBeChangedEvenIfMalformedServerClaimsEligible()throws Exception {
        JsonObject b=AssignmentTestData.orders();b.getAsJsonArray("orders").get(0).getAsJsonObject().addProperty("status","in_progress");Assignments.Order order=Assignments.Orders.parse(b,"").rows.get(0);
        Assignments.Options page=Assignments.Options.parse(AssignmentTestData.options(),order,"");rejects(()->AssignmentAction.create(AssignmentTestData.all(),1,page,page.rows.get(0),"Reason"));
    }
    @Test public void deniedOrUnverifiedOwnerAndPhoneScopeHaveNoOperationalAccess(){
        assertFalse(AssignmentTestData.access("drivers.read").canReadOrders());
        for(boolean phone:new boolean[]{false,true}){
            JsonObject body=JsonParser.parseString("{\"ok\":true,\"admin\":{\"accountId\":\"owner\",\"role\":\"owner\",\"permissions\":[\"*\"]}}").getAsJsonObject();
            assertFalse(AdminAccess.fromResponse(body,"owner").canReadOrders());
            JsonObject mfa=new JsonObject();mfa.addProperty("required",true);mfa.addProperty("verified",true);mfa.addProperty("method","totp");mfa.addProperty("expiresAt",System.currentTimeMillis()+100000);
            body.getAsJsonObject("admin").add("mfa",mfa);if(phone)body.getAsJsonObject("admin").addProperty("testScope","owner_phone");
            assertEquals(!phone,AdminAccess.fromResponse(body,"owner").canAssign(false));
        }
    }
}
