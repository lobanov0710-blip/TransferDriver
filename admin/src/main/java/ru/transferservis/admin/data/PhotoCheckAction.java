package ru.transferservis.admin.data;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/** One immutable foreground command. An explicit retry preserves every original field and key. */
public final class PhotoCheckAction {
    public final String accountId,key,kind,mode,reason;
    public final long scope;
    public final PhotoControl.Vehicle vehicle;
    private PhotoCheckAction(AdminAccess access,long scope,PhotoControl.Vehicle vehicle,String kind,String mode,String reason){
        accountId=access.accountId;this.scope=scope;this.vehicle=vehicle;this.kind=kind;this.mode=mode;this.reason=reason;key="photo-control-"+UUID.randomUUID();
    }
    public static PhotoCheckAction create(AdminAccess access,long scope,PhotoControl.Checks page,String kind,String mode,String reason)throws IOException{
        if(access==null||!access.canManagePhotoChecks()||scope<1||page==null||!page.vehicle.canRequest()||!Arrays.asList("complaint","manual").contains(kind)||!Arrays.asList("after_current_trip","immediate").contains(mode))throw new IOException("Photo check unavailable");
        if(reason==null||reason.matches("(?s).*[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F].*"))throw new IOException("Reason required");
        int a=0,b=reason.length();while(a<b&&space(reason.charAt(a)))a++;while(b>a&&space(reason.charAt(b-1)))b--;String note=reason.substring(a,b);
        if(note.isEmpty()||note.length()>500)throw new IOException("Reason required");return new PhotoCheckAction(access,scope,page.vehicle,kind,mode,note);
    }
    private static boolean space(char c){return Character.isWhitespace(c)||Character.isSpaceChar(c)||c=='\uFEFF';}
    public JsonObject body(){JsonObject b=new JsonObject();b.addProperty("kind",kind);b.addProperty("reason",reason);b.addProperty("restrictionMode",mode);b.addProperty("expectedRevision",vehicle.revision);return b;}
    void validateAck(JsonObject body)throws IOException{
        try{JsonObject c=body.getAsJsonObject("photoCheck");if(!OwnerMfa.trueField(body,"ok")||!AdminSession.string(c,"id").matches("photo-check_[A-Za-z0-9_-]{8,120}")
            ||!vehicle.id.equals(AdminSession.string(c,"vehicleId"))||!kind.equals(AdminSession.string(c,"kind"))||!mode.equals(AdminSession.string(c,"restrictionMode"))
            ||!"pending".equals(AdminSession.string(c,"result"))||Assignments.revision(c,"revision")!=1||Assignments.revision(c,"vehicleRevision")!=vehicle.revision+1)throw new IOException("Unconfirmed photo check");
        }catch(RuntimeException e){throw new IOException("Unconfirmed photo check");}
    }
}
