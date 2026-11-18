package ru.transferservis.admin.data;

import android.content.Context;
import java.io.IOException;
import okhttp3.Interceptor;

/** Read-only foreground client. No refresh, replay, disk data or private document request. */
public final class VehicleClassificationRepository {
    private final AdminSessionStore store;private final AdminApiClient api;private AdminSession bound;private long generation;
    public VehicleClassificationRepository(Context context){this(context,null);}
    VehicleClassificationRepository(Context context,Interceptor transport){store=new AdminSessionStore(context);api=new AdminApiClient(chain->{
        Long scope=chain.request().tag(Long.class);if(scope==null)throw new AdminApiClient.Failure(401);
        AdminSession session=require(scope);if(!("Bearer "+session.accessToken).equals(chain.request().header("Authorization")))throw new AdminApiClient.Failure(401);
        return chain.proceed(chain.request());},transport);}
    public synchronized long bind(String account)throws IOException {cancel();AdminSession s=store.load();if(s==null||!s.accountId.equals(account)||!s.accessUsable()||!s.refreshUsable())throw new AdminApiClient.Failure(401);bound=s;return generation;}
    public synchronized void cancel(){generation++;bound=null;api.cancelPending();}
    public synchronized boolean current(long scope){AdminSession actual=store.load();return scope==generation&&bound!=null&&bound.same(actual)&&bound.accessToken.equals(actual.accessToken)&&actual.accessUsable()&&actual.refreshUsable();}
    private synchronized AdminSession require(long scope)throws IOException {if(!current(scope))throw new AdminApiClient.Failure(401,"session_changed");return bound;}
    private AdminAccess authorize(long scope)throws IOException {
        AdminSession s=require(scope);AdminAccess a=AdminAccess.fromResponse(api.assignmentMe(s.accessToken,scope),s.accountId);require(scope);
        if(a==null||!a.canReadClassification())throw new AdminApiClient.Failure(403);return a;
    }
    public static final class Read {
        public final AdminAccess access;public final VehicleClassification.Page page;public final VehicleClassification.Rule rule;
        Read(AdminAccess a,VehicleClassification.Page p,VehicleClassification.Rule r){access=a;page=p;rule=r;}
    }
    public Read read(long scope,String vehicle,String cursor,Long expectedRevision)throws IOException {
        AdminAccess access=authorize(scope);
        VehicleClassification.Page page=VehicleClassification.Page.parse(api.classification(require(scope).accessToken,vehicle,cursor,scope),vehicle,cursor,expectedRevision);require(scope);
        VehicleClassification.Rule rule=null;
        if(page.decision!=null&&page.decision.ruleVersion!=null&&access.canReadVehicles()){
            access=authorize(scope);if(!access.canReadVehicles())throw new AdminApiClient.Failure(403);
            rule=VehicleClassification.Rule.parse(api.classificationRule(require(scope).accessToken,page.decision.ruleVersion,scope),page.decision.ruleVersion);require(scope);
            if(!rule.make.equals(page.verified.make)||(page.decision.vehicleClass!=null&&!page.decision.vehicleClass.equals(rule.vehicleClass)))throw new IOException("Unconfirmed rule metadata");
        }
        if(!access.canReadClassification())throw new AdminApiClient.Failure(403);return new Read(access,page,rule);
    }
}
