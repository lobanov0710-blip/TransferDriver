package ru.transferservis.admin.data;

import android.content.Context;
import java.io.IOException;
import okhttp3.Interceptor;

/** Independent one-shot operational client. No refresh/replay or account substitution. */
public final class AssignmentRepository {
    private final AdminSessionStore store;
    private final AdminApiClient api;
    private AdminSession bound;
    private long generation;
    public AssignmentRepository(Context context){this(context,null);}
    AssignmentRepository(Context context,Interceptor transport){
        store=new AdminSessionStore(context);
        api=new AdminApiClient(chain->{
            Long scope=chain.request().tag(Long.class);if(scope==null)throw new AdminApiClient.Failure(401);
            AdminSession expected=require(scope);
            if(!("Bearer "+expected.accessToken).equals(chain.request().header("Authorization")))throw new AdminApiClient.Failure(401);
            return chain.proceed(chain.request());
        },transport);
    }
    public synchronized long bind()throws IOException {
        cancel();AdminSession session=store.load();if(session==null||!session.accessUsable()||!session.refreshUsable())throw new AdminApiClient.Failure(401);
        bound=session;return generation;
    }
    public synchronized void cancel(){generation++;bound=null;api.cancelPending();}
    public synchronized boolean current(long scope){
        AdminSession actual=store.load();return scope==generation&&bound!=null&&bound.same(actual)&&bound.accessToken.equals(actual.accessToken)
                &&actual.accessUsable()&&actual.refreshUsable();
    }
    private synchronized AdminSession require(long scope)throws IOException {if(!current(scope))throw new AdminApiClient.Failure(401,"session_changed");return bound;}
    private AdminAccess authorize(long scope)throws IOException {
        AdminSession session=require(scope);AdminAccess access=AdminAccess.fromResponse(api.assignmentMe(session.accessToken,scope),session.accountId);require(scope);
        if(access==null||!access.canReadOrders())throw new AdminApiClient.Failure(403);return access;
    }
    public static final class Read<T>{public final AdminAccess access;public final T data;Read(AdminAccess access,T data){this.access=access;this.data=data;}}
    public Read<Assignments.Orders> orders(long scope,String status,String cursor)throws IOException {
        AdminAccess access=authorize(scope);AdminSession session=require(scope);
        Assignments.Orders page=Assignments.Orders.parse(api.assignmentOrders(session.accessToken,status,cursor,scope),cursor);require(scope);return new Read<>(access,page);
    }
    public Read<Assignments.Options> options(long scope,Assignments.Order order,String cursor)throws IOException {
        AdminAccess access=authorize(scope);if(!access.canAssign(order.needsReassignPermission()))throw new AdminApiClient.Failure(403);
        Assignments.Options page=Assignments.Options.parse(api.assignmentOptions(require(scope).accessToken,order.id,cursor,scope),order,cursor);
        require(scope);return new Read<>(access,page);
    }
    public AdminAccess apply(AssignmentAction action)throws IOException {
        AdminAccess access=authorize(action.scope);
        if(!access.accountId.equals(action.accountId)||!access.canAssign(action.reassign))throw new AdminApiClient.Failure(403);
        action.validateAck(api.assignmentCommand(require(action.scope).accessToken,action));require(action.scope);return access;
    }
}
