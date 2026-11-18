package ru.transferservis.admin.data;

import com.google.gson.*;
import java.io.IOException;
import java.util.*;

/** Strict, immutable metadata. A historical candidate never supplies current verification. */
public final class VehicleClassification {
    private VehicleClassification() { }
    public static final class Parameters {
        public final String make,model,color,powertrain,bodyType;
        public final Long year;
        private Parameters(JsonObject value,boolean verified)throws IOException {
            keys(value,verified?new String[]{"make","model","modelYear","powertrain","bodyType"}:new String[]{"make","model","modelYear","color"});
            make=text(value,"make",80);model=text(value,"model",80);year=number(value,"modelYear",!verified);
            if(year!=null&&year>9999)throw invalid();
            color=verified?null:nullable(value,"color",80);
            powertrain=verified?choice(value,"powertrain","combustion","hybrid","battery_electric"):null;
            bodyType=verified?choice(value,"bodyType","sedan","crossover","liftback","minivan","other"):null;
        }
    }
    public static final class Decision {
        public final String outcome,vehicleClass,ruleVersion;
        public final List<String> reasons;
        private Decision(JsonObject o)throws IOException {
            keys(o,"outcome","vehicleClass","ruleVersion","reasonCodes");outcome=choice(o,"outcome","candidate","manual_review","excluded");
            vehicleClass=nullable(o,"vehicleClass",16);ruleVersion=nullableVersion(o,"ruleVersion");
            if(vehicleClass!=null&&!Arrays.asList("comfort","business","minivan").contains(vehicleClass))throw invalid();
            List<String> values=new ArrayList<>();
            for(JsonElement e:array(o,"reasonCodes",32)){if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString()||!e.getAsString().matches("[a-z][a-z0-9_]{0,79}")||values.contains(e.getAsString()))throw invalid();values.add(e.getAsString());}
            if("candidate".equals(outcome)?vehicleClass==null||ruleVersion==null||!values.isEmpty():vehicleClass!=null||values.isEmpty())throw invalid();
            reasons=Collections.unmodifiableList(values);
        }
    }
    public static final class Evidence {
        public final String id;public final long revision;
        private Evidence(JsonObject o)throws IOException {keys(o,"documentId","expectedRevision");id=text(o,"documentId",120);if(!id.matches("[A-Za-z0-9_-]{8,120}"))throw invalid();revision=Assignments.revision(o,"expectedRevision");}
    }
    public static final class Condition {
        public final String id,text;
        private Condition(JsonObject o)throws IOException {keys(o,"id","text");id=text(o,"id",128);text=text(o,"text",1000);}
    }
    public static final class History {
        public final String id,ruleVersion,outcome;public final long revision,createdAt;
        private History(JsonObject o)throws IOException {keys(o,"assessmentId","vehicleRevision","ruleVersion","outcome","createdAt");
            id=text(o,"assessmentId",128);revision=Assignments.revision(o,"vehicleRevision");createdAt=Assignments.revision(o,"createdAt");
            ruleVersion=nullableVersion(o,"ruleVersion");outcome=choice(o,"outcome","candidate","manual_review","excluded");}
    }
    public static final class Page {
        public final String vehicleId,pinnedRuleVersion,nextCursor;
        public final long revision;public final Long inputRevision;
        public final Parameters declared,verified;public final Decision decision;
        public final List<Evidence> evidence;public final List<Condition> conditions;public final List<History> history;
        private Page(JsonObject body,String vehicle,String cursor,Long expectedRevision)throws IOException {
            keys(body,"ok","vehicleId","vehicleRevision","inputRevision","declaredParameters","verifiedParameters","pinnedRuleVersion","classification","evidence","conditions","history","nextHistoryCursor","requestId");ok(body);
            vehicleId=Assignments.vehicleId(text(body,"vehicleId",108));if(!vehicleId.equals(Assignments.vehicleId(vehicle)))throw invalid();
            revision=Assignments.revision(body,"vehicleRevision");if(expectedRevision!=null&&revision!=expectedRevision)throw new AdminApiClient.Failure(409,"vehicle_revision_conflict");
            inputRevision=number(body,"inputRevision",true);pinnedRuleVersion=nullableVersion(body,"pinnedRuleVersion");
            declared=new Parameters(object(body,"declaredParameters"),false);
            verified=field(body,"verifiedParameters").isJsonNull()?null:new Parameters(object(body,"verifiedParameters"),true);
            decision=field(body,"classification").isJsonNull()?null:new Decision(object(body,"classification"));
            if((decision==null)!=(verified==null))throw invalid();
            if(verified!=null&&(inputRevision==null||!verified.make.equals(declared.make)||!verified.model.equals(declared.model)||!verified.year.equals(declared.year)))throw invalid();
            List<Evidence> documents=new ArrayList<>();Set<String> ids=new HashSet<>();
            for(JsonElement e:array(body,"evidence",8)){Evidence v=new Evidence(e.getAsJsonObject());if(!ids.add(v.id))throw invalid();documents.add(v);}
            if(verified==null?!documents.isEmpty():documents.size()<2)throw invalid();evidence=Collections.unmodifiableList(documents);
            conditions=conditions(body);List<History> rows=new ArrayList<>();ids.clear();long prior=revision;
            for(JsonElement e:array(body,"history",50)){History h=new History(e.getAsJsonObject());if(!ids.add(h.id)||h.revision>prior)throw invalid();prior=h.revision;rows.add(h);}
            history=Collections.unmodifiableList(rows);nextCursor=nullable(body,"nextHistoryCursor",1024);
            if(nextCursor!=null&&(cursor(nextCursor).isEmpty()||nextCursor.equals(cursor)||rows.size()!=50))throw invalid();
        }
        public static Page parse(JsonObject body,String vehicle,String cursor,Long revision)throws IOException {try{return new Page(body,vehicle,cursor,revision);}catch(RuntimeException invalid){throw invalid();}}
    }
    public static final class Rule {
        public final String version,make,model,vehicleClass;public final long minYear,revision;
        public final List<String> allowedBodyTypes;
        public final List<Condition> conditions;
        private Rule(JsonObject response,String expected)throws IOException {
            keys(response,"ok","rule","requestId");ok(response);JsonObject o=object(response,"rule");
            keys(o,"version","make","model","revision","minYear","vehicleClass","allowedBodyTypes","conditions");
            version=version(text(o,"version",128));if(!version.equals(version(expected)))throw invalid();
            make=text(o,"make",80);model=text(o,"model",80);revision=Assignments.revision(o,"revision");minYear=Assignments.revision(o,"minYear");if(minYear>9999)throw invalid();
            vehicleClass=choice(o,"vehicleClass","comfort","business","minivan");conditions=conditions(o);
            List<String> bodies=null;if(!field(o,"allowedBodyTypes").isJsonNull()){
                bodies=new ArrayList<>();
                Set<String> seen=new HashSet<>();for(JsonElement e:array(o,"allowedBodyTypes",4)){
                    if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString()||!Arrays.asList("sedan","crossover","liftback","minivan","other").contains(e.getAsString())||!seen.add(e.getAsString()))throw invalid();bodies.add(e.getAsString());
                }if(seen.isEmpty())throw invalid();
            }if(!"comfort".equals(vehicleClass)&&bodies!=null&&bodies.contains("liftback"))throw invalid();allowedBodyTypes=bodies==null?null:Collections.unmodifiableList(bodies);
        }
        public static Rule parse(JsonObject body,String version)throws IOException {try{return new Rule(body,version);}catch(RuntimeException invalid){throw invalid();}}
    }
    static Decision decision(JsonObject value)throws IOException {return new Decision(value);}
    static String cursor(String value)throws IOException {if(value==null)return "";if(!value.isEmpty()&&!value.matches("[A-Za-z0-9_-]{1,1024}"))throw invalid();return value;}
    static String version(String value)throws IOException {if(value==null||!value.matches("[A-Za-z0-9_.:-]{1,128}")||value.equals(".")||value.equals(".."))throw invalid();return value;}
    private static String nullableVersion(JsonObject o,String key)throws IOException {String v=nullable(o,key,128);return v==null?null:version(v);}
    private static List<Condition> conditions(JsonObject o)throws IOException {
        List<Condition> values=new ArrayList<>();Set<String> ids=new HashSet<>();for(JsonElement e:array(o,"conditions",32)){Condition c=new Condition(e.getAsJsonObject());if(!ids.add(c.id))throw invalid();values.add(c);}return Collections.unmodifiableList(values);
    }
    private static void keys(JsonObject o,String... names)throws IOException {if(o==null||!new HashSet<>(Arrays.asList(names)).equals(o.keySet()))throw invalid();}
    private static JsonElement field(JsonObject o,String key)throws IOException {JsonElement e=o.get(key);if(e==null)throw invalid();return e;}
    private static JsonObject object(JsonObject o,String key)throws IOException {JsonElement e=field(o,key);if(!e.isJsonObject())throw invalid();return e.getAsJsonObject();}
    private static JsonArray array(JsonObject o,String key,int max)throws IOException {JsonElement e=field(o,key);if(!e.isJsonArray()||e.getAsJsonArray().size()>max)throw invalid();return e.getAsJsonArray();}
    private static Long number(JsonObject o,String key,boolean nullable)throws IOException {return nullable&&field(o,key).isJsonNull()?null:Assignments.revision(o,key);}
    private static String nullable(JsonObject o,String key,int max)throws IOException {JsonElement e=field(o,key);if(e.isJsonNull())return null;if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString())throw invalid();String v=e.getAsString();if(v.length()>max||v.matches("(?s).*[\\x00-\\x1F\\x7F].*"))throw invalid();return v;}
    private static String text(JsonObject o,String key,int max)throws IOException {String v=nullable(o,key,max);if(v==null||v.isEmpty())throw invalid();return v;}
    private static String choice(JsonObject o,String key,String... options)throws IOException {String v=text(o,key,40);if(!Arrays.asList(options).contains(v))throw invalid();return v;}
    private static void ok(JsonObject o)throws IOException {if(!OwnerMfa.trueField(o,"ok")||!text(o,"requestId",128).matches("req_[A-Za-z0-9_-]{8,120}"))throw invalid();}
    private static IOException invalid(){return new IOException("Invalid classification metadata");}
}
