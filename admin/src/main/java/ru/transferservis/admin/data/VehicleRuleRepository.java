package ru.transferservis.admin.data;
import android.content.Context;
import java.io.IOException;
import okhttp3.Interceptor;

/** OWNER and actual MFA on every operation; no token refresh or automatic write/read replay. */
public final class VehicleRuleRepository {
    private final AdminSessionStore store;private final AdminApiClient api;private AdminSession bound;private long generation;
    public VehicleRuleRepository(Context c){this(c,null);}
    VehicleRuleRepository(Context c,Interceptor transport){store=new AdminSessionStore(c);api=new AdminApiClient(chain->{Long scope=chain.request().tag(Long.class);if(scope==null)throw new AdminApiClient.Failure(401);AdminSession s=require(scope);if(!("Bearer "+s.accessToken).equals(chain.request().header("Authorization")))throw new AdminApiClient.Failure(401);return chain.proceed(chain.request());},transport);}
    public synchronized long bind(String account)throws IOException {cancel();AdminSession s=store.load();if(s==null||!s.accountId.equals(account)||!s.accessUsable()||!s.refreshUsable())throw new AdminApiClient.Failure(401);bound=s;return generation;}
    public synchronized void cancel(){generation++;bound=null;api.cancelPending();}
    public synchronized boolean current(long scope){AdminSession s=store.load();return scope==generation&&bound!=null&&bound.same(s)&&bound.accessToken.equals(s.accessToken)&&s.accessUsable()&&s.refreshUsable();}
    private synchronized AdminSession require(long scope)throws IOException {if(!current(scope))throw new AdminApiClient.Failure(401,"session_changed");return bound;}
    public AdminAccess access(long scope)throws IOException {AdminSession s=require(scope);AdminAccess a=AdminAccess.fromResponse(api.assignmentMe(s.accessToken,scope),s.accountId);require(scope);if(a==null||!a.canManageVehicleRules())throw new AdminApiClient.Failure(403);return a;}
    public static final class Read {public final AdminAccess access;public final VehicleRuleAction.Snapshot snapshot;Read(AdminAccess a,VehicleRuleAction.Snapshot s){access=a;snapshot=s;}}
    public Read read(long scope,String make,String model)throws IOException {AdminAccess a=access(scope);String m=VehicleAssessment.text(make,80),n=VehicleAssessment.text(model,80);VehicleRuleAction.Snapshot s=VehicleRuleAction.Snapshot.parse(a,scope,m,n,api.classificationRules(require(scope).accessToken,m,n,scope));require(scope);return new Read(a,s);}
    public VehicleClassification.Rule apply(VehicleRuleAction action)throws IOException {AdminAccess a=access(action.scope);if(!a.accountId.equals(action.accountId))throw new AdminApiClient.Failure(403);VehicleClassification.Rule r=action.acknowledge(api.createVehicleRule(require(action.scope).accessToken,action));require(action.scope);return r;}
}
