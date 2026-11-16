package ru.transferservis.admin.data;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.UUID;

/** One foreground intent: original actor/scope, immutable body/revision and a single idempotency key. */
public final class AssignmentAction {
    public final String accountId,key,reason;
    public final long scope;
    public final Assignments.Order order;
    public final Assignments.Option option;
    public final boolean reassign;
    private AssignmentAction(AdminAccess access,long scope,Assignments.Order order,Assignments.Option option,String reason) {
        accountId=access.accountId;this.scope=scope;this.order=order;this.option=option;this.reason=reason;
        reassign=order.reassignable();key="assignment-"+UUID.randomUUID();
    }
    public static AssignmentAction create(AdminAccess access,long scope,Assignments.Options page,Assignments.Option option,String reason)throws IOException {
        if(access==null||page==null||option==null||scope<1||!page.rows.contains(option)||!option.eligible
                ||!(page.order.newAssignment()||page.order.reassignable())||!access.canAssign(page.order.needsReassignPermission())
                ||page.order.revision==Assignments.MAX_REVISION)throw new IOException("Assignment unavailable");
        if(reason==null||reason.matches("(?s).*[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F].*"))throw new IOException("Reason required");
        int first=0,last=reason.length();while(first<last&&space(reason.charAt(first)))first++;while(last>first&&space(reason.charAt(last-1)))last--;
        String note=reason.substring(first,last);if(note.isEmpty()||note.length()>500)throw new IOException("Reason required");
        return new AssignmentAction(access,scope,page.order,option,note);
    }
    private static boolean space(char c){return Character.isWhitespace(c)||Character.isSpaceChar(c)||c=='\uFEFF';}
    public JsonObject body(){JsonObject body=new JsonObject();body.addProperty("driverId",option.driverId);body.addProperty("vehicleId",option.vehicleId);
        body.addProperty("reason",reason);body.addProperty("expectedRevision",order.revision);return body;}
    void validateAck(JsonObject body)throws IOException {
        try {JsonObject result=body.getAsJsonObject("assignment");
            if(!AdminSession.string(result,"id").matches("assignment_[A-Za-z0-9_-]{8,100}")||!"active".equals(AdminSession.string(result,"state"))
                ||Assignments.revision(result,"revision")<1||!OwnerMfa.trueField(body,"ok")||!order.id.equals(AdminSession.string(result,"orderId"))
                ||!option.driverId.equals(AdminSession.string(result,"driverId"))||!option.vehicleId.equals(AdminSession.string(result,"vehicleId"))
                ||Assignments.revision(result,"orderRevision")!=order.revision+1)throw new IOException("Unconfirmed assignment");
        }catch(RuntimeException invalid){throw new IOException("Unconfirmed assignment");}
    }
}
