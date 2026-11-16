package ru.transferservis.admin.data;

import android.content.Context;
import java.io.IOException;
import okhttp3.Interceptor;

/** Current-session metadata client. No refresh, automatic mutation replay, private material or disk snapshots. */
public final class PhotoControlRepository {
    private final AdminSessionStore store;private final AdminApiClient api;private AdminSession bound;private long generation;
    public PhotoControlRepository(Context context){this(context,null);}
    PhotoControlRepository(Context context,Interceptor transport){store=new AdminSessionStore(context);api=new AdminApiClient(chain->{
        Long scope=chain.request().tag(Long.class);if(scope==null)throw new AdminApiClient.Failure(401);AdminSession expected=require(scope);
        if(!("Bearer "+expected.accessToken).equals(chain.request().header("Authorization")))throw new AdminApiClient.Failure(401);
        return chain.proceed(chain.request());},transport);}
    public synchronized long bind()throws IOException{cancel();AdminSession s=store.load();if(s==null||!s.accessUsable()||!s.refreshUsable())throw new AdminApiClient.Failure(401);bound=s;return generation;}
    public synchronized void cancel(){generation++;bound=null;api.cancelPending();}
    public synchronized boolean current(long scope){AdminSession actual=store.load();return scope==generation&&bound!=null&&bound.same(actual)&&bound.accessToken.equals(actual.accessToken)&&actual.accessUsable()&&actual.refreshUsable();}
    private synchronized AdminSession require(long scope)throws IOException{if(!current(scope))throw new AdminApiClient.Failure(401,"session_changed");return bound;}
    private AdminAccess authorize(long scope,boolean verify)throws IOException{AdminSession s=require(scope);AdminAccess a=AdminAccess.fromResponse(api.assignmentMe(s.accessToken,scope),s.accountId);require(scope);
        if(a==null||!a.canReadVehicles()||(verify&&!a.canManagePhotoChecks()))throw new AdminApiClient.Failure(403);return a;}
    public static final class Read<T>{public final AdminAccess access;public final T data;Read(AdminAccess a,T d){access=a;data=d;}}
    public Read<PhotoControl.Vehicles> vehicles(long scope,String status,String cursor)throws IOException{AdminAccess a=authorize(scope,false);PhotoControl.Vehicles page=PhotoControl.Vehicles.parse(api.photoVehicles(require(scope).accessToken,status,cursor,scope),cursor);require(scope);return new Read<>(a,page);}
    public Read<PhotoControl.Checks> checks(long scope,PhotoControl.Vehicle vehicle,String cursor)throws IOException{AdminAccess a=authorize(scope,true);PhotoControl.Checks page=PhotoControl.Checks.parse(api.photoChecks(require(scope).accessToken,vehicle.id,cursor,scope),vehicle,cursor);require(scope);return new Read<>(a,page);}
    public void apply(PhotoCheckAction action)throws IOException{AdminAccess a=authorize(action.scope,true);if(!a.accountId.equals(action.accountId))throw new AdminApiClient.Failure(403);
        action.validateAck(api.photoCommand(require(action.scope).accessToken,action));require(action.scope);}
}
