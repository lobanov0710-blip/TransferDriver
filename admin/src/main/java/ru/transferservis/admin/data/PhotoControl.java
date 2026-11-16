package ru.transferservis.admin.data;

import com.google.gson.*;
import java.io.IOException;
import java.util.*;

/** The approved metadata-only wire projection, never private photo material. */
public final class PhotoControl {
    private PhotoControl() { }
    public static final List<String> STATUSES=Collections.unmodifiableList(Arrays.asList("draft","pending_review","approved","photo_check_required","suspended","rejected"));
    public static final class Vehicle {
        public final String id,driverId,make,model,plate,status;
        public final long revision;
        public final Long lastPassedAt,nextDueAt;
        private Vehicle(JsonObject r)throws IOException {
            keys(r,"id","driverId","make","model","plate","status","revision","lastPhotoCheckPassedAt","nextPhotoCheckDueAt");
            id=Assignments.vehicleId(text(r,"id",108));driverId=Assignments.driverId(text(r,"driverId",100));
            make=text(r,"make",80);model=text(r,"model",80);plate=text(r,"plate",32);status=choice(r,"status",STATUSES.toArray(new String[0]));revision=Assignments.revision(r,"revision");
            lastPassedAt=timestamp(r,"lastPhotoCheckPassedAt",true);nextDueAt=timestamp(r,"nextPhotoCheckDueAt",true);
        }
        private Vehicle(Vehicle v,long revision){id=v.id;driverId=v.driverId;make=v.make;model=v.model;plate=v.plate;status=v.status;lastPassedAt=v.lastPassedAt;nextDueAt=v.nextDueAt;this.revision=revision;}
        public Vehicle afterOwnMutation(){return new Vehicle(this,revision+1);}
        public boolean canRequest(){return Arrays.asList("approved","photo_check_required","suspended").contains(status)&&revision<Assignments.MAX_REVISION;}
    }
    public static final class Vehicles {
        public final List<Vehicle> rows;public final String nextCursor;
        private Vehicles(JsonObject body,String requested)throws IOException {
            keys(body,"ok","vehicles","nextCursor","requestId");ok(body);List<Vehicle> values=new ArrayList<>();Set<String> ids=new HashSet<>();
            for(JsonElement item:array(body,"vehicles")){Vehicle v=new Vehicle(item.getAsJsonObject());if(!ids.add(v.id))throw invalid();values.add(v);}
            rows=Collections.unmodifiableList(values);nextCursor=next(body,requested,rows.isEmpty());
        }
        public static Vehicles parse(JsonObject body,String requested)throws IOException{try{return new Vehicles(body,requested);}catch(RuntimeException e){throw invalid();}}
    }
    public static final class Check {
        public final String id,kind,restrictionMode,result;public final long revision,dueAt,graceUntil,createdAt,updatedAt;
        private Check(JsonObject r,String vehicle)throws IOException {
            keys(r,"id","vehicleId","kind","restrictionMode","result","revision","dueAt","graceUntil","createdAt","updatedAt");
            id=text(r,"id",132);if(!id.matches("photo-check_[A-Za-z0-9_-]{8,120}")||!vehicle.equals(text(r,"vehicleId",108)))throw invalid();
            kind=choice(r,"kind","monthly","complaint","manual");restrictionMode=choice(r,"restrictionMode","grace_period","after_current_trip","immediate");
            result=choice(r,"result","pending","passed","failed","manual_review");revision=Assignments.revision(r,"revision");
            dueAt=timestamp(r,"dueAt",false);graceUntil=timestamp(r,"graceUntil",false);createdAt=timestamp(r,"createdAt",false);updatedAt=timestamp(r,"updatedAt",false);
            if(graceUntil<dueAt||updatedAt<createdAt)throw invalid();
        }
    }
    public static final class Checks {
        public final Vehicle vehicle;public final List<Check> rows;public final String nextCursor;
        private Checks(JsonObject body,Vehicle expected,String requested)throws IOException {
            keys(body,"ok","vehicleId","vehicleRevision","checks","nextCursor","requestId");ok(body);
            if(!expected.id.equals(text(body,"vehicleId",108)))throw invalid();
            if(Assignments.revision(body,"vehicleRevision")!=expected.revision)throw new AdminApiClient.Failure(409,"vehicle_revision_conflict");
            vehicle=expected;List<Check> values=new ArrayList<>();Set<String> ids=new HashSet<>();
            for(JsonElement item:array(body,"checks")){Check c=new Check(item.getAsJsonObject(),expected.id);if(!ids.add(c.id))throw invalid();values.add(c);}
            rows=Collections.unmodifiableList(values);nextCursor=next(body,requested,rows.isEmpty());
        }
        public static Checks parse(JsonObject body,Vehicle expected,String requested)throws IOException{try{return new Checks(body,expected,requested);}catch(RuntimeException e){throw invalid();}}
    }
    private static void keys(JsonObject o,String... allowed)throws IOException{if(o==null||!Arrays.asList(allowed).containsAll(o.keySet()))throw invalid();}
    private static JsonElement field(JsonObject o,String key)throws IOException{JsonElement v=o.get(key);if(v==null)throw invalid();return v;}
    private static String text(JsonObject o,String key,int max)throws IOException{JsonElement v=field(o,key);if(!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isString())throw invalid();String s=v.getAsString();if(s.isEmpty()||s.length()>max||s.matches("(?s).*[\\x00-\\x1F\\x7F].*"))throw invalid();return s;}
    private static String choice(JsonObject o,String key,String... choices)throws IOException{String s=text(o,key,40);if(!Arrays.asList(choices).contains(s))throw invalid();return s;}
    private static Long timestamp(JsonObject o,String key,boolean nullable)throws IOException{if(nullable&&field(o,key).isJsonNull())return null;return Assignments.revision(o,key);}
    private static Iterable<JsonElement> array(JsonObject o,String key)throws IOException{JsonElement v=field(o,key);if(!v.isJsonArray()||v.getAsJsonArray().size()>50)throw invalid();return v.getAsJsonArray();}
    private static void ok(JsonObject o)throws IOException{if(!OwnerMfa.trueField(o,"ok")||!text(o,"requestId",128).matches("req_[A-Za-z0-9_-]{8,120}"))throw invalid();}
    private static String next(JsonObject o,String requested,boolean empty)throws IOException{JsonElement v=field(o,"nextCursor");if(v.isJsonNull())return null;String s=text(o,"nextCursor",512);Assignments.cursor(s);if(s.equals(requested)||empty)throw invalid();return s;}
    private static IOException invalid(){return new IOException("Invalid photo-control data");}
}
