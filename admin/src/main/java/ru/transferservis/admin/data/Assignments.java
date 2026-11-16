package ru.transferservis.admin.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/** Minimal operational read contract. No contact/document fields or mutable local eligibility. */
public final class Assignments {
    private Assignments() { }
    public static final long MAX_REVISION = 9_007_199_254_740_991L;
    public static final class Order {
        public final String id, from, to, date, tariff, status;
        public final long revision;
        public final Current assignment;
        private Order(JsonObject row) throws IOException {
            keys(row,"id","from","to","date","tariff","status","revision","assignment");
            id=orderId(text(row,"id",120));from=nullable(row,"from",200);to=nullable(row,"to",200);
            date=text(row,"date",10);try{LocalDate.parse(date);}catch(RuntimeException invalid){throw invalid();}
            tariff=nullable(row,"tariff",20);if(tariff!=null&&!Arrays.asList("comfort","business","minivan").contains(tariff))throw invalid();
            status=choice(row,"status","new","taken","in_progress");revision=revision(row,"revision");
            JsonElement a=field(row,"assignment");assignment=a.isJsonNull()?null:new Current(a.getAsJsonObject());
        }
        public boolean newAssignment(){return "new".equals(status)&&assignment==null;}
        public boolean reassignable(){return "taken".equals(status)&&assignment!=null;}
        public boolean needsReassignPermission(){return !"new".equals(status)||assignment!=null;}
    }
    public static final class Current {
        public final String id,driverId,vehicleId,phase;
        public final long revision;
        private Current(JsonObject row)throws IOException {
            keys(row,"id","driverId","vehicleId","revision","phase");
            id=text(row,"id",120);if(!id.matches("assignment_[A-Za-z0-9_-]{8,100}"))throw invalid();
            driverId=driverId(text(row,"driverId",100));vehicleId=vehicleId(text(row,"vehicleId",108));
            revision=revision(row,"revision");phase=choice(row,"phase","assigned","en_route_to_pickup","arrived","in_trip");
        }
    }
    public static final class Orders {
        public final List<Order> rows;
        public final String nextCursor;
        private Orders(JsonObject body,String requested)throws IOException {
            response(body,"orders");List<Order> parsed=new ArrayList<>();HashSet<String> ids=new HashSet<>();
            for(JsonElement row:array(body,"orders",50)) {Order order=new Order(row.getAsJsonObject());if(!ids.add(order.id))throw invalid();parsed.add(order);}
            rows=Collections.unmodifiableList(parsed);nextCursor=next(body,requested,rows.isEmpty());
        }
        public static Orders parse(JsonObject body,String requested)throws IOException {try{return new Orders(body,requested);}catch(RuntimeException e){throw invalid();}}
    }
    public static final class Option {
        public final String driverId,fullName,vehicleId,make,model,plate,status,state,activeVehicleId;
        public final boolean eligible;
        public final List<String> blockers;
        private Option(JsonObject row)throws IOException {
            keys(row,"driverId","fullName","vehicleId","make","model","plate","status","availability","eligible","blockers");
            driverId=driverId(text(row,"driverId",100));vehicleId=vehicleId(text(row,"vehicleId",108));
            fullName=nullable(row,"fullName",100);make=text(row,"make",80);model=text(row,"model",80);plate=text(row,"plate",32);
            status=nullable(row,"status",40);if(status!=null&&!Arrays.asList("draft","pending_review","approved","photo_check_required","suspended","rejected").contains(status))throw invalid();
            JsonObject availability=field(row,"availability").getAsJsonObject();keys(availability,"state","activeVehicleId");
            state=choice(availability,"state","off_line","on_line");activeVehicleId=nullable(availability,"activeVehicleId",108);
            if(activeVehicleId!=null)vehicleId(activeVehicleId);if("on_line".equals(state)&&activeVehicleId==null)throw invalid();
            JsonElement flag=field(row,"eligible");if(!flag.isJsonPrimitive()||!flag.getAsJsonPrimitive().isBoolean())throw invalid();eligible=flag.getAsBoolean();
            List<String> reasons=new ArrayList<>();HashSet<String> seen=new HashSet<>();
            for(JsonElement item:array(row,"blockers",20)) {
                if(!item.isJsonPrimitive()||!item.getAsJsonPrimitive().isString())throw invalid();String code=item.getAsString();
                if(!code.matches("[a-z0-9_]{1,80}")||!seen.add(code))throw invalid();reasons.add(code);
            }
            blockers=Collections.unmodifiableList(reasons);
            if(eligible!=blockers.isEmpty()||(eligible&&(!"approved".equals(status)||("on_line".equals(state)&&!vehicleId.equals(activeVehicleId)))))throw invalid();
        }
    }
    public static final class Options {
        public final Order order;
        public final List<Option> rows;
        public final String nextCursor;
        private Options(JsonObject body,Order expected,String requested)throws IOException {
            keys(body,"ok","orderId","orderRevision","options","nextCursor","requestId");ok(body);
            if(!expected.id.equals(orderId(text(body,"orderId",120))))throw invalid();
            if(revision(body,"orderRevision")!=expected.revision)throw new AdminApiClient.Failure(409,"order_revision_conflict");
            order=expected;List<Option> parsed=new ArrayList<>();HashSet<String> pairs=new HashSet<>();
            for(JsonElement row:array(body,"options",50)) {Option option=new Option(row.getAsJsonObject());if(!pairs.add(option.driverId+"\u0000"+option.vehicleId))throw invalid();parsed.add(option);}
            rows=Collections.unmodifiableList(parsed);nextCursor=next(body,requested,rows.isEmpty());
        }
        public static Options parse(JsonObject body,Order expected,String requested)throws IOException {try{return new Options(body,expected,requested);}catch(RuntimeException e){throw invalid();}}
    }
    static String orderId(String id)throws IOException {if(id==null||!id.matches("[A-Za-z0-9_-]{8,120}"))throw invalid();return id;}
    static String driverId(String id)throws IOException {if(id==null||!id.matches("staff_[A-Za-z0-9_-]{8,94}"))throw invalid();return id;}
    static String vehicleId(String id)throws IOException {if(id==null||!id.matches("vehicle_[A-Za-z0-9_-]{8,100}"))throw invalid();return id;}
    static String cursor(String cursor)throws IOException {if(cursor!=null&&!cursor.isEmpty()&&!cursor.matches("[A-Za-z0-9_-]{1,512}"))throw invalid();return cursor==null?"":cursor;}
    private static String next(JsonObject body,String requested,boolean empty)throws IOException {
        String next=nullable(body,"nextCursor",512);if(next!=null&&(cursor(next).isEmpty()||next.equals(requested)||empty))throw invalid();return next;
    }
    private static void response(JsonObject body,String list)throws IOException {keys(body,"ok",list,"nextCursor","requestId");ok(body);}
    private static void ok(JsonObject body)throws IOException {if(!OwnerMfa.trueField(body,"ok"))throw invalid();}
    private static void keys(JsonObject row,String... keys)throws IOException {if(row==null||!Arrays.asList(keys).containsAll(row.keySet()))throw invalid();}
    private static JsonElement field(JsonObject row,String key)throws IOException {JsonElement value=row.get(key);if(value==null)throw invalid();return value;}
    private static Iterable<JsonElement> array(JsonObject row,String key,int max)throws IOException {
        JsonElement value=field(row,key);if(!value.isJsonArray()||value.getAsJsonArray().size()>max)throw invalid();return value.getAsJsonArray();
    }
    private static String text(JsonObject row,String key,int max)throws IOException {String value=nullable(row,key,max);if(value==null||value.isEmpty())throw invalid();return value;}
    private static String nullable(JsonObject row,String key,int max)throws IOException {
        JsonElement item=field(row,key);if(item.isJsonNull())return null;
        if(!item.isJsonPrimitive()||!item.getAsJsonPrimitive().isString())throw invalid();String value=item.getAsString();
        if(value.length()>max||value.matches("(?s).*[\\x00-\\x1F\\x7F].*"))throw invalid();return value;
    }
    private static String choice(JsonObject row,String key,String... choices)throws IOException {String value=text(row,key,40);if(!Arrays.asList(choices).contains(value))throw invalid();return value;}
    static long revision(JsonObject row,String key)throws IOException {
        JsonElement value=field(row,key);if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw invalid();
        try {long n=value.getAsBigDecimal().longValueExact();if(n<1||n>MAX_REVISION)throw invalid();return n;}catch(RuntimeException e){throw invalid();}
    }
    private static IOException invalid(){return new IOException("Invalid assignment data");}
}
