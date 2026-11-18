package ru.transferservis.admin.data;
import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Original OWNER intent. No caller identity, generated IDs or vehicle admission flags. */
public final class VehicleRuleAction {
    public static final class Snapshot {
        public final String accountId,make,model;public final long scope;public final VehicleClassification.Rule rule;
        Snapshot(AdminAccess a,long scope,String make,String model,VehicleClassification.Rule rule){accountId=a.accountId;this.scope=scope;this.make=make;this.model=model;this.rule=rule;}
        static Snapshot parse(AdminAccess a,long scope,String make,String model,JsonObject b)throws IOException {
            try{if(!new HashSet<>(Arrays.asList("ok","rules","nextCursor","requestId")).equals(b.keySet())||!OwnerMfa.trueField(b,"ok")||!AdminSession.string(b,"requestId").matches("req_[A-Za-z0-9_-]{8,120}")||!b.get("nextCursor").isJsonNull()||!b.get("rules").isJsonArray()||b.getAsJsonArray("rules").size()>1)throw invalid();
                VehicleClassification.Rule r=null;if(!b.getAsJsonArray("rules").isEmpty()){JsonObject o=b.getAsJsonArray("rules").get(0).getAsJsonObject(),wrapper=new JsonObject();wrapper.addProperty("ok",true);wrapper.add("requestId",b.get("requestId"));wrapper.add("rule",o);r=VehicleClassification.Rule.parse(wrapper,AdminSession.string(o,"version"));if(!r.make.equals(make)||!r.model.equals(model))throw invalid();}
                return new Snapshot(a,scope,make,model,r);
            }catch(RuntimeException malformed){throw invalid();}
        }
    }
    public final Snapshot snapshot;public final String accountId,key,make,model,vehicleClass,reason;public final long scope,minYear;
    public final List<String> bodies,conditions;private final String payload;
    private VehicleRuleAction(Snapshot s,String vehicleClass,long minYear,List<String> bodies,List<String> conditions,String reason,JsonObject b){snapshot=s;accountId=s.accountId;scope=s.scope;make=s.make;model=s.model;this.vehicleClass=vehicleClass;this.minYear=minYear;this.bodies=bodies==null?null:Collections.unmodifiableList(new ArrayList<>(bodies));this.conditions=Collections.unmodifiableList(new ArrayList<>(conditions));this.reason=reason;key="vehicle-rule-"+UUID.randomUUID();payload=b.toString();}
    public static VehicleRuleAction create(AdminAccess a,Snapshot s,String make,String model,String year,String vehicleClass,List<String> bodies,List<String> conditions,String reason)throws IOException {
        if(a==null||!a.canManageVehicleRules()||s==null||s.scope<1||s.rule!=null&&s.rule.revision>=Assignments.MAX_REVISION||!a.accountId.equals(s.accountId)||!s.make.equals(VehicleAssessment.text(make,80))||!s.model.equals(VehicleAssessment.text(model,80)))throw invalid();
        String y=VehicleAssessment.text(year,4);if(!y.matches("[0-9]{1,4}")||Long.parseLong(y)<1||!Arrays.asList("comfort","business","minivan").contains(vehicleClass))throw invalid();long minimum=Long.parseLong(y);
        if(bodies!=null&&(bodies.isEmpty()||bodies.size()>4||new HashSet<>(bodies).size()!=bodies.size()))throw invalid();
        if(bodies!=null)for(String type:bodies)if(!Arrays.asList("sedan","crossover","minivan","other").contains(type))throw invalid();
        if(!"minivan".equals(vehicleClass)&&bodies==null)throw invalid();
        if("comfort".equals(vehicleClass)&&!Arrays.asList("sedan","crossover").containsAll(bodies))throw invalid();
        if("business".equals(vehicleClass)){String required="Li Auto".equals(s.make)&&Arrays.asList("L7","L9").contains(s.model)?"crossover":"sedan";if(bodies.size()!=1||!required.equals(bodies.get(0)))throw invalid();}
        if(conditions==null||conditions.size()>32||new HashSet<>(conditions).size()!=conditions.size())throw invalid();for(String c:conditions)if(!VehicleAssessment.text(c,1000).equals(c))throw invalid();
        String note=VehicleAssessment.text(reason,1000);JsonObject b=new JsonObject();b.addProperty("make",s.make);b.addProperty("model",s.model);b.addProperty("minYear",minimum);b.addProperty("vehicleClass",vehicleClass);
        JsonArray types=new JsonArray(),rows=new JsonArray();if(bodies!=null)for(String type:bodies)types.add(type);for(String c:conditions)rows.add(c);b.add("allowedBodyTypes",bodies==null?JsonNull.INSTANCE:types);b.add("conditions",rows);
        if(s.rule==null)b.add("expectedCurrentVersion",JsonNull.INSTANCE);else b.addProperty("expectedCurrentVersion",s.rule.version);b.addProperty("reason",note);
        if(b.toString().getBytes(StandardCharsets.UTF_8).length>16384)throw invalid();return new VehicleRuleAction(s,vehicleClass,minimum,bodies,conditions,note,b);
    }
    public JsonObject body(){return JsonParser.parseString(payload).getAsJsonObject();}
    VehicleClassification.Rule acknowledge(JsonObject response)throws IOException {
        try{JsonObject o=response.getAsJsonObject("rule");VehicleClassification.Rule r=VehicleClassification.Rule.parse(response,AdminSession.string(o,"version"));
            if(!r.make.equals(make)||!r.model.equals(model)||r.minYear!=minYear||!r.vehicleClass.equals(vehicleClass)||!Objects.equals(r.allowedBodyTypes,bodies)||r.conditions.size()!=conditions.size()||snapshot.rule!=null&&(r.version.equals(snapshot.rule.version)||r.revision<=snapshot.rule.revision))throw invalid();
            for(int i=0;i<conditions.size();i++)if(!r.conditions.get(i).text.equals(conditions.get(i))||!r.conditions.get(i).id.equals(r.version+":"+(i+1)))throw invalid();return r;
        }catch(RuntimeException malformed){throw invalid();}
    }
    private static IOException invalid(){return new IOException("Invalid vehicle rule intent");}
}
