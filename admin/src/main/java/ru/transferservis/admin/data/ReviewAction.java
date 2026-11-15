package ru.transferservis.admin.data;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.UUID;

/** An explicit retry keeps exactly the original actor, target, revision, body and key. */
public final class ReviewAction {
    public final String accountId,driverId,type,targetId,decision,note,key;
    public final long revision;
    private ReviewAction(String actor,RegistrationReview review,String type,String id,long revision,String decision,String note) {
        accountId=actor;driverId=review.driverId;this.type=type;targetId=id;this.revision=revision;
        this.decision=decision;this.note=note;key=UUID.randomUUID().toString();
    }
    public static ReviewAction start(AdminAccess access,RegistrationReview review) throws IOException {
        if(!access.canStartReview()||!("submitted".equals(review.status)||"changes_required".equals(review.status)))throw new IOException("Review unavailable");
        return new ReviewAction(access.accountId,review,"start",review.driverId,review.revision,"","");
    }
    public static ReviewAction decide(AdminAccess access,RegistrationReview review,RegistrationReview.Item item,String decision,String reason) throws IOException {
        if(!"under_review".equals(review.status)||(item==null?!access.canFinalizeReview():!access.canReviewItem(item)))throw new IOException("Review unavailable");
        if(item!=null&&(review.item(item.id)!=item||!item.pending()))throw new IOException("Review unavailable");
        String positive=item!=null&&!item.isVehicle()?"verified":"approved";
        if(!positive.equals(decision)&&!"changes_required".equals(decision)&&!"rejected".equals(decision))throw new IOException("Invalid decision");
        String note=reason==null?"":reason.trim();
        if(note.length()>1000||note.matches("(?s).*[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F].*")
                ||(!positive.equals(decision)&&note.isEmpty()))throw new IOException("Review reason required");
        return new ReviewAction(access.accountId,review,item==null?"complete":item.type,item==null?review.driverId:item.id,
                item==null?review.revision:item.revision,decision,note);
    }
    public JsonObject body(){JsonObject body=new JsonObject();body.addProperty("expectedRevision",revision);
        if(!"start".equals(type)){body.addProperty("decision",decision);if(!note.isEmpty())body.addProperty("note",note);}return body;}
}
