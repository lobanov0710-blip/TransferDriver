package ru.transferservis.admin.data;
import android.content.Context;
import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import okhttp3.Interceptor;

/** Current-session form client. No token refresh, automatic replay or persistent snapshots. */
public final class VehicleAssessmentRepository {
    private final AdminSessionStore store;private final AdminApiClient api;private AdminSession bound;private long generation;
    public VehicleAssessmentRepository(Context c){this(c,null);}
    VehicleAssessmentRepository(Context c,Interceptor transport){store=new AdminSessionStore(c);api=new AdminApiClient(chain->{Long scope=chain.request().tag(Long.class);if(scope==null)throw new AdminApiClient.Failure(401);AdminSession s=require(scope);if(!("Bearer "+s.accessToken).equals(chain.request().header("Authorization")))throw new AdminApiClient.Failure(401);return chain.proceed(chain.request());},transport);}
    public synchronized long bind(String account)throws IOException {cancel();AdminSession s=store.load();if(s==null||!s.accountId.equals(account)||!s.accessUsable()||!s.refreshUsable())throw new AdminApiClient.Failure(401);bound=s;return generation;}
    public synchronized void cancel(){generation++;bound=null;api.cancelPending();}
    public synchronized boolean current(long scope){AdminSession a=store.load();return scope==generation&&bound!=null&&bound.same(a)&&bound.accessToken.equals(a.accessToken)&&a.accessUsable()&&a.refreshUsable();}
    private synchronized AdminSession require(long scope)throws IOException {if(!current(scope))throw new AdminApiClient.Failure(401,"session_changed");return bound;}
    private AdminAccess authorize(long scope,boolean exception)throws IOException {AdminSession s=require(scope);AdminAccess a=AdminAccess.fromResponse(api.assignmentMe(s.accessToken,scope),s.accountId);require(scope);if(a==null||!a.canReadClassification()||exception&&!a.canApproveVehicleException())throw new AdminApiClient.Failure(403);return a;}
    public static final class Read<T>{public final AdminAccess access;public final T data;Read(AdminAccess a,T d){access=a;data=d;}}
    public Read<VehicleAssessment.Context> load(long scope,String driver,String vehicle)throws IOException {
        AdminAccess a=authorize(scope,false);RegistrationReview registration=RegistrationReview.parse(api.classificationRegistration(require(scope).accessToken,driver,scope),driver);require(scope);
        VehicleClassification.Page page=VehicleClassification.Page.parse(api.classification(require(scope).accessToken,vehicle,"",scope),vehicle,"",null);require(scope);
        return new Read<>(a,new VehicleAssessment.Context(a,scope,registration,vehicle,page));
    }
    private VehicleClassification.Rule currentRule(long scope,String make,String model)throws IOException {
        AdminAccess a=authorize(scope,false);if(!a.canReadVehicles())throw new AdminApiClient.Failure(403);
        JsonObject b=api.classificationRules(require(scope).accessToken,make,model,scope);require(scope);
        try{if(!new HashSet<>(Arrays.asList("ok","rules","nextCursor","requestId")).equals(b.keySet())||!OwnerMfa.trueField(b,"ok")||!AdminSession.string(b,"requestId").matches("req_[A-Za-z0-9_-]{8,120}")||!b.get("nextCursor").isJsonNull()||!b.get("rules").isJsonArray()||b.getAsJsonArray("rules").size()>1)throw VehicleAssessment.invalid();
            if(b.getAsJsonArray("rules").isEmpty())return null;JsonObject r=b.getAsJsonArray("rules").get(0).getAsJsonObject();JsonObject wrapper=new JsonObject();wrapper.addProperty("ok",true);wrapper.add("requestId",b.get("requestId"));wrapper.add("rule",r);
            VehicleClassification.Rule rule=VehicleClassification.Rule.parse(wrapper,AdminSession.string(r,"version"));if(!rule.make.equals(make)||!rule.model.equals(model))throw VehicleAssessment.invalid();return rule;
        }catch(RuntimeException invalid){throw VehicleAssessment.invalid();}
    }
    public Read<VehicleAssessment.Selection> select(long scope,VehicleAssessment.Context c,VehicleAssessment.Parameters p,boolean exception,String basis)throws IOException {
        AdminAccess a=authorize(scope,exception);if(c.scope!=scope||!a.accountId.equals(c.accountId))throw new AdminApiClient.Failure(401);
        String target=c.page.pinnedRuleVersion;VehicleClassification.Rule rule=null;List<VehicleClassification.Condition> conditions=target==null?Collections.emptyList():c.page.conditions;
        if(p.keysChanged(c)){rule=currentRule(scope,p.make,p.model);target=rule==null?null:rule.version;conditions=rule==null?Collections.emptyList():rule.conditions;}
        else if(target!=null&&c.page.decision!=null&&!Objects.equals(target,c.page.decision.ruleVersion)){
            a=authorize(scope,false);if(!a.canReadVehicles())throw new AdminApiClient.Failure(403);rule=VehicleClassification.Rule.parse(api.classificationRule(require(scope).accessToken,target,scope),target);conditions=rule.conditions;
        }
        String model=exception?VehicleAssessment.text(basis,80):"";
        if(exception){rule=currentRule(scope,p.make,model);if(rule==null)throw new AdminApiClient.Failure(404,"rule_not_found");conditions=rule.conditions;}
        require(scope);return new Read<>(a,new VehicleAssessment.Selection(c,p,exception,target,rule,model,conditions));
    }
    public byte[] preview(long scope,VehicleAssessment.Context c,VehicleAssessment.Document d)throws IOException {
        AdminAccess a=authorize(scope,false);if(c.scope!=scope||!c.accountId.equals(a.accountId)||!c.contains(d)||!a.canReadResource(d.item))throw new AdminApiClient.Failure(403);
        byte[] bytes=api.resource(require(scope).accessToken,c.driverId,d.item,scope);require(scope);return bytes;
    }
    public VehicleAssessmentAction.Ack apply(VehicleAssessmentAction action)throws IOException {
        AdminAccess a=authorize(action.scope,action.selection.exception);if(!a.accountId.equals(action.accountId))throw new AdminApiClient.Failure(403);
        VehicleAssessmentAction.Ack ack=action.validateAck(api.classificationReview(require(action.scope).accessToken,action));require(action.scope);return ack;
    }
}
