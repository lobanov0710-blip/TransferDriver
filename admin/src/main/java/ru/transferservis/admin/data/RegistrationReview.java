package ru.transferservis.admin.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Immutable, memory-only projection. No object keys, download URLs or file contents. */
public final class RegistrationReview {
    public final String driverId, fullName, region, birthDate, status, identityStatus;
    public final long revision;
    public final List<Item> items;
    public final List<History> history;
    private RegistrationReview(JsonObject profile, List<Item> items, List<History> history) throws IOException {
        driverId=driverId(text(profile,"driverId",128)); fullName=text(profile,"fullName",200);
        region=text(profile,"region",120); birthDate=text(profile,"birthDate",32);
        status=text(profile,"registrationStatus",32); identityStatus=text(profile,"identityStatus",40);
        if(!isRegistrationStatus(status))throw invalid();
        revision=positive(profile,"revision"); this.items=Collections.unmodifiableList(items);this.history=Collections.unmodifiableList(history);
    }
    public static final class Item {
        public final String type,id,vehicleId,kind,status,vin,plate,make,model,usageBasis;
        public final long revision;
        private Item(String type,JsonObject object) throws IOException {
            this.type=type;id=targetId(text(object,"id",120));vehicleId=text(object,"vehicleId",120);
            kind=text(object,"kind",80);status=text(object,"status",40);revision=positive(object,"revision");
            vin=text(object,"vin",32);plate=text(object,"plate",32);make=text(object,"make",100);
            model=text(object,"model",100);usageBasis=text(object,"usageBasis",32);
            if(!vehicleId.isEmpty())targetId(vehicleId);
        }
        public boolean isVehicle(){return "vehicle".equals(type);}
        public boolean pending(){return (isVehicle()?"pending_review":"pending").equals(status);}
    }
    public static final class History {
        public final String type,id,decision,note;
        public final long createdAt;
        private History(JsonObject object) throws IOException {
            type=text(object,"targetType",40);id=text(object,"targetId",128);decision=text(object,"decision",40);
            note=text(object,"note",1000);createdAt=positive(object,"createdAt");
        }
    }
    public static RegistrationReview parse(JsonObject response,String expectedDriver) throws IOException {
        try {
            if(!OwnerMfa.trueField(response,"ok"))throw invalid();
            JsonObject body=response.getAsJsonObject("registration"),profile=body.getAsJsonObject("profile");
            if(!driverId(text(profile,"driverId",128)).equals(driverId(expectedDriver)))throw invalid();
            List<Item> items=new ArrayList<>();Set<String> ids=new HashSet<>();ids.add(expectedDriver);
            for(String type:new String[]{"vehicle","document","vehicle_photo"}) {
                String key="vehicle".equals(type)?"vehicles":"document".equals(type)?"documents":"photos";
                JsonArray rows=body.getAsJsonArray(key);if(rows==null||rows.size()>500)throw invalid();
                for(JsonElement row:rows){Item item=new Item(type,row.getAsJsonObject());if(!ids.add(item.id))throw invalid();items.add(item);}
            }
            Set<String> vehicles=new HashSet<>();for(Item item:items)if(item.isVehicle())vehicles.add(item.id);
            for(Item item:items)if(("vehicle_photo".equals(item.type)&&item.vehicleId.isEmpty())
                    ||(!item.vehicleId.isEmpty()&&!vehicles.contains(item.vehicleId)))throw invalid();
            JsonArray rows=body.getAsJsonArray("reviews");if(rows==null||rows.size()>200)throw invalid();
            List<History> history=new ArrayList<>();for(JsonElement row:rows){History h=new History(row.getAsJsonObject());if(!ids.contains(h.id))throw invalid();history.add(h);}
            return new RegistrationReview(profile,items,history);
        }catch(RuntimeException invalid){throw invalid();}
    }
    public Item item(String id){for(Item item:items)if(item.id.equals(id))return item;return null;}
    public static boolean isRegistrationStatus(String value){return java.util.Arrays.asList("draft","submitted","under_review","changes_required","approved","rejected").contains(value);}
    static String driverId(String value) throws IOException {if(value==null||!value.matches("staff_[A-Za-z0-9_-]{8,94}"))throw invalid();return value;}
    static String targetId(String value) throws IOException {if(value==null||!value.matches("[A-Za-z0-9_-]{8,120}"))throw invalid();return value;}
    static String text(JsonObject object,String field,int max) throws IOException {
        if(object==null)throw invalid();JsonElement value=object.get(field);if(value==null||value.isJsonNull())return "";
        if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString()||value.getAsString().length()>max)throw invalid();return value.getAsString();
    }
    static long positive(JsonObject object,String key) throws IOException {long n=OwnerMfa.timestamp(object,key);if(n<1||n>9_007_199_254_740_991L)throw invalid();return n;}
    private static IOException invalid(){return new IOException("Invalid registration response");}
}
