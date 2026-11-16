package ru.transferservis.admin.data;
import com.google.gson.*;
final class AssignmentTestData {
    static final String ORDER="order_fixture001",CAR="vehicle_fixture001",DRIVER="staff_driver001";
    static JsonObject order(){return JsonParser.parseString("{\"id\":\""+ORDER+"\",\"from\":\"Origin\",\"to\":null,\"date\":\"2030-01-01\",\"tariff\":\"comfort\",\"status\":\"new\",\"revision\":7,\"assignment\":null}").getAsJsonObject();}
    static JsonObject orders(){JsonObject body=new JsonObject();body.addProperty("ok",true);JsonArray rows=new JsonArray();rows.add(order());body.add("orders",rows);body.add("nextCursor",JsonNull.INSTANCE);return body;}
    static JsonObject option(){return JsonParser.parseString("{\"driverId\":\""+DRIVER+"\",\"fullName\":null,\"vehicleId\":\""+CAR+"\",\"make\":\"Synthetic\",\"model\":\"Car\",\"plate\":\"SYNTH-01\",\"status\":\"approved\",\"availability\":{\"state\":\"off_line\",\"activeVehicleId\":null},\"eligible\":true,\"blockers\":[]}").getAsJsonObject();}
    static JsonObject options(){JsonObject body=new JsonObject();body.addProperty("ok",true);body.addProperty("orderId",ORDER);body.addProperty("orderRevision",7);JsonArray rows=new JsonArray();rows.add(option());body.add("options",rows);body.add("nextCursor",JsonNull.INSTANCE);return body;}
    static AdminAccess access(String... permissions){JsonObject body=JsonParser.parseString("{\"ok\":true,\"admin\":{\"accountId\":\"staff_operator001\",\"role\":\"dispatcher\",\"permissions\":[]}}").getAsJsonObject();for(String p:permissions)body.getAsJsonObject("admin").getAsJsonArray("permissions").add(p);return AdminAccess.fromResponse(body,"staff_operator001");}
    static AdminAccess all(){return access("orders.read","drivers.read","vehicles.read","orders.assign","orders.reassign");}
    static Assignments.Order parsedOrder()throws java.io.IOException{return Assignments.Orders.parse(orders(),"").rows.get(0);}
    static Assignments.Options parsedOptions()throws java.io.IOException{return Assignments.Options.parse(options(),parsedOrder(),"");}
}
