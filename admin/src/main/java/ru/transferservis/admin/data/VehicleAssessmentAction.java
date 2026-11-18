package ru.transferservis.admin.data;
import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Original foreground intent. Explicit retries preserve the complete body and key. */
public final class VehicleAssessmentAction {
    public final String accountId,key,reason;public final long scope;
    public final VehicleAssessment.Context context;public final VehicleAssessment.Selection selection;
    private final String payload;
    private VehicleAssessmentAction(AdminAccess access,VehicleAssessment.Selection s,String reason,JsonObject body){accountId=access.accountId;scope=s.context.scope;context=s.context;selection=s;this.reason=reason;key="vehicle-review-"+UUID.randomUUID();payload=body.toString();}
    public static VehicleAssessmentAction create(AdminAccess a,VehicleAssessment.Selection s,VehicleAssessment.Parameters p,boolean exception,String basis,
            VehicleAssessment.Document front,VehicleAssessment.Document back,List<String> decisions,Set<String> waived,boolean waiveYear,String reason)throws IOException {
        if(a==null||!a.canReadClassification()||s==null||!a.accountId.equals(s.context.accountId)||!s.matches(p,exception,basis)||(exception&&!a.canApproveVehicleException()))throw VehicleAssessment.invalid();
        VehicleAssessment.Context c=s.context;if(c.scope<1||c.page.revision>=Assignments.MAX_REVISION||(p.keysChanged(c)&&c.page.inputRevision!=null&&c.page.inputRevision>=Assignments.MAX_REVISION)||(!p.keysChanged(c)&&c.page.inputRevision==null))throw VehicleAssessment.invalid();
        if(front==null||back==null||!c.contains(front)||!c.contains(back)||!"sts_front".equals(front.kind)||!"sts_back".equals(back.kind)||front.id.equals(back.id))throw VehicleAssessment.invalid();
        if(decisions==null||decisions.size()!=s.conditions.size()||waived==null||(!exception&&(waiveYear||!waived.isEmpty()))||exception&&s.rule==null)throw VehicleAssessment.invalid();
        String note=VehicleAssessment.text(reason,1000);JsonObject b=new JsonObject();b.addProperty("expectedRevision",c.page.revision);
        if(c.page.inputRevision==null)b.add("expectedInputRevision",JsonNull.INSTANCE);else b.addProperty("expectedInputRevision",c.page.inputRevision);
        if(s.targetPinned==null)b.add("expectedRuleVersion",JsonNull.INSTANCE);else b.addProperty("expectedRuleVersion",s.targetPinned);
        b.addProperty("mode",exception?"exception":"standard");b.add("parameters",p.json());JsonArray evidence=new JsonArray();evidence.add(front.json());evidence.add(back.json());b.add("evidence",evidence);
        JsonArray conditions=new JsonArray(),waivers=new JsonArray();Set<String> accepted=new HashSet<>();
        for(int i=0;i<s.conditions.size();i++){String value=decisions.get(i),id=s.conditions.get(i).id;
            if(!Arrays.asList("","satisfied","unmet").contains(value))throw VehicleAssessment.invalid();
            if(!value.isEmpty()){JsonObject item=new JsonObject();item.addProperty("conditionId",id);item.addProperty("result",value);conditions.add(item);}
            if(waived.contains(id)){if(!exception||!"unmet".equals(value))throw VehicleAssessment.invalid();waivers.add(id);accepted.add(id);}
        }
        if(!accepted.equals(waived))throw VehicleAssessment.invalid();b.add("conditions",conditions);b.addProperty("reason",note);
        if(exception){JsonObject x=new JsonObject();x.addProperty("basisRuleVersion",s.rule.version);x.addProperty("yearBelowMinimum",waiveYear);x.add("waivedConditionIds",waivers);b.add("exception",x);}else b.add("exception",JsonNull.INSTANCE);
        if(b.toString().getBytes(StandardCharsets.UTF_8).length>16384)throw VehicleAssessment.invalid();return new VehicleAssessmentAction(a,s,note,b);
    }
    public JsonObject body(){return JsonParser.parseString(payload).getAsJsonObject();}
    public static final class Ack {
        public final VehicleClassification.Decision decision;public final boolean dispatcher;public final long revision,inputRevision;
        private Ack(VehicleClassification.Decision d,boolean dispatch,long rev,long input){decision=d;dispatcher=dispatch;revision=rev;inputRevision=input;}
    }
    Ack validateAck(JsonObject b)throws IOException {
        try{
            Set<String> keys=new HashSet<>(Arrays.asList("ok","vehicleId","vehicleRevision","inputRevision","classification","requiresDispatcherReassignment","requestId"));
            if(b==null||!keys.equals(b.keySet())||!OwnerMfa.trueField(b,"ok")||!context.vehicleId.equals(AdminSession.string(b,"vehicleId"))||!AdminSession.string(b,"requestId").matches("req_[A-Za-z0-9_-]{8,120}"))throw VehicleAssessment.invalid();
            long rev=Assignments.revision(b,"vehicleRevision"),input=Assignments.revision(b,"inputRevision");
            long expected=(context.page.inputRevision==null?0:context.page.inputRevision)+(selection.parameters.keysChanged(context)?1:0);
            if(rev!=context.page.revision+1||input!=expected||!b.get("requiresDispatcherReassignment").isJsonPrimitive()||!b.getAsJsonPrimitive("requiresDispatcherReassignment").isBoolean())throw VehicleAssessment.invalid();
            VehicleClassification.Decision d=VehicleClassification.decision(b.getAsJsonObject("classification"));String expectedRule=selection.exception?selection.rule.version:selection.targetPinned;
            if(!Objects.equals(d.ruleVersion,expectedRule))throw VehicleAssessment.invalid();return new Ack(d,b.get("requiresDispatcherReassignment").getAsBoolean(),rev,input);
        }catch(RuntimeException invalid){throw VehicleAssessment.invalid();}
    }
}
