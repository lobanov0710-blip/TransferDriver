package ru.transferservis.admin.ui;

import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import ru.transferservis.admin.R;
import ru.transferservis.admin.data.RegistrationReview;

/** Renders one item at a time; all private views are excluded from hierarchy saving. */
final class ReviewPanel {
    private final AdminActivity activity;
    private final AdminViewModel model;
    private final Spinner items;
    private AdminViewModel.State state;
    private String selected="";
    private AlertDialog confirmation;
    ReviewPanel(AdminActivity activity,AdminViewModel model){
        this.activity=activity;this.model=model;items=activity.findViewById(R.id.reviewItems);
        for(int id:new int[]{R.id.reviewStart,R.id.reviewPreview,R.id.reviewApproveItem,R.id.reviewReturnItem,R.id.reviewRejectItem,
                R.id.reviewApprove,R.id.reviewReturn,R.id.reviewReject,R.id.reviewRetry})activity.findViewById(id).setFilterTouchesWhenObscured(true);
        activity.findViewById(R.id.reviewCard).setSaveFromParentEnabled(false);
        items.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onNothingSelected(AdapterView<?> parent){}
            @Override public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                if(state!=null&&state.reviewScreen!=null&&position<state.reviewScreen.registration.items.size()){
                    String next=state.reviewScreen.registration.items.get(position).id;
                    if(!next.equals(selected))((EditText)activity.findViewById(R.id.reviewNote)).setText("");
                    selected=next;renderItem();
                }
            }
        });
        activity.findViewById(R.id.reviewClose).setOnClickListener(v->model.closeReview());
        activity.findViewById(R.id.reviewReload).setOnClickListener(v->{if(screen()!=null)model.openReview(screen().registration.driverId);});
        activity.findViewById(R.id.reviewRetry).setOnClickListener(v->model.retryReview());
        activity.findViewById(R.id.reviewStart).setOnClickListener(v->confirm(null,"start"));
        activity.findViewById(R.id.reviewPreview).setOnClickListener(v->{if(item()!=null)model.preview(item().id);});
        activity.findViewById(R.id.reviewApproveItem).setOnClickListener(v->{RegistrationReview.Item i=item();if(i!=null)confirm(i.id,i.isVehicle()?"approved":"verified");});
        activity.findViewById(R.id.reviewReturnItem).setOnClickListener(v->{if(item()!=null)confirm(item().id,"changes_required");});
        activity.findViewById(R.id.reviewRejectItem).setOnClickListener(v->{if(item()!=null)confirm(item().id,"rejected");});
        activity.findViewById(R.id.reviewApprove).setOnClickListener(v->confirm(null,"approved"));
        activity.findViewById(R.id.reviewReturn).setOnClickListener(v->confirm(null,"changes_required"));
        activity.findViewById(R.id.reviewReject).setOnClickListener(v->confirm(null,"rejected"));
    }
    private ReviewScreen screen(){return state==null?null:state.reviewScreen;}
    private RegistrationReview.Item item(){return screen()==null?null:screen().registration.item(selected);}
    void render(AdminViewModel.State state){
        this.state=state;ReviewScreen screen=screen();
        boolean show=screen!=null&&state.access!=null&&!state.loading;
        activity.findViewById(R.id.reviewCard).setVisibility(show?View.VISIBLE:View.GONE);
        if(!show){clearViews();return;}
        RegistrationReview r=screen.registration;
        text(R.id.reviewProfile,activity.getString(R.string.review_profile,r.fullName,r.region,r.birthDate,status(r.status)));
        text(R.id.reviewIdentity,activity.getString(R.string.review_identity,status(r.identityStatus)));
        List<String> labels=new ArrayList<>();int chosen=0;
        for(int n=0;n<r.items.size();n++){RegistrationReview.Item i=r.items.get(n);labels.add(title(i));if(i.id.equals(selected))chosen=n;}
        items.setAdapter(new ArrayAdapter<>(activity,android.R.layout.simple_spinner_dropdown_item,labels));
        selected=r.items.isEmpty()?"":r.items.get(chosen).id;items.setSelection(chosen);
        boolean available=screen.uncertain==null;
        enabled(R.id.reviewStart,available&&state.access.canStartReview()&&("submitted".equals(r.status)||"changes_required".equals(r.status)));
        boolean complete=available&&"under_review".equals(r.status)&&state.access.canFinalizeReview();
        enabled(R.id.reviewApprove,complete&&"verified".equals(r.identityStatus));
        enabled(R.id.reviewReturn,complete);enabled(R.id.reviewReject,complete);
        activity.findViewById(R.id.reviewRetry).setVisibility(available?View.GONE:View.VISIBLE);
        items.setEnabled(available);
        StringBuilder history=new StringBuilder(activity.getString(R.string.review_history_title));history.append('\n');
        if(r.history.isEmpty())history.append(activity.getString(R.string.review_history_empty));
        for(RegistrationReview.History h:r.history){RegistrationReview.Item target=r.item(h.id);
            history.append(activity.getString(R.string.review_history_row,
                android.text.format.DateFormat.getDateFormat(activity).format(new Date(h.createdAt)),
                target==null?activity.getString(R.string.review_registration_label):title(target),status(h.decision),h.note));}
        text(R.id.reviewHistory,history.toString());renderItem();
    }
    private void renderItem(){
        RegistrationReview.Item i=item();ReviewScreen s=screen();boolean available=state!=null&&!state.loading&&state.access!=null&&s!=null&&s.uncertain==null;
        boolean pending=available&&"under_review".equals(s.registration.status)&&i!=null&&i.pending()&&state.access.canReviewItem(i);
        boolean canRead=available&&i!=null&&state.access.canReadResource(i);
        enabled(R.id.reviewPreview,canRead);
        boolean viewed=i!=null&&s!=null&&i.id.equals(s.previewId)&&s.preview!=null;
        enabled(R.id.reviewApproveItem,pending&&(i.isVehicle()||(canRead&&viewed)));
        enabled(R.id.reviewReturnItem,pending);enabled(R.id.reviewRejectItem,pending);
        String details=i==null?"":activity.getString(R.string.review_item_state,status(i.status));
        if(i!=null&&i.isVehicle())details+="\n"+activity.getString(R.string.review_vehicle_details,i.make,i.model,i.plate,i.vin,i.usageBasis);
        text(R.id.reviewItemDetails,details);
        ImageView image=activity.findViewById(R.id.reviewImage);image.setImageBitmap(viewed&&canRead?s.preview:null);
        image.setVisibility(viewed&&canRead?View.VISIBLE:View.GONE);
    }
    private void confirm(String itemId,String decision){
        if(screen()==null||state.loading||screen().uncertain!=null)return;
        RegistrationReview expected=screen().registration;
        EditText field=activity.findViewById(R.id.reviewNote);String note=field.getText().toString();
        if(("changes_required".equals(decision)||"rejected".equals(decision))&&note.trim().isEmpty()){
            field.setError(activity.getString(R.string.reason_required));return;
        }
        confirmation=new AlertDialog.Builder(activity).setTitle(R.string.review_confirm)
            .setMessage("start".equals(decision)?activity.getString(R.string.review_confirm_start):
                (itemId==null?activity.getString(R.string.review_registration_label):title(expected.item(itemId)))+"\n"+status(decision))
            .setNegativeButton(R.string.review_no,null).setPositiveButton(R.string.review_yes,(dialog,which)->{
                field.setText("");if(screen()==null||screen().registration!=expected)return;
                if("start".equals(decision))model.startReview();else model.decideReview(itemId,decision,note);
            }).create();
        if(confirmation.getWindow()!=null)confirmation.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        confirmation.show();confirmation.getButton(AlertDialog.BUTTON_POSITIVE).setFilterTouchesWhenObscured(true);
    }
    void clear(){if(confirmation!=null){confirmation.dismiss();confirmation=null;}clearViews();state=null;selected="";}
    private void clearViews(){for(int id:new int[]{R.id.reviewProfile,R.id.reviewIdentity,R.id.reviewItemDetails,R.id.reviewHistory,R.id.reviewNote})text(id,"");
        ((ImageView)activity.findViewById(R.id.reviewImage)).setImageDrawable(null);items.setAdapter(null);}
    private void text(int id,String value){((TextView)activity.findViewById(id)).setText(value);}
    private void enabled(int id,boolean enabled){activity.findViewById(id).setEnabled(enabled);}
    private String title(RegistrationReview.Item i){if(i==null)return "";
        return i.isVehicle()?activity.getString(R.string.review_vehicle_label)+" · "+i.plate:activity.getString(R.string.review_material_label,kind(i.kind));}
    private String kind(String kind){
        switch(kind){
            case "passport_main":return activity.getString(R.string.kind_passport_main);
            case "passport_registration":return activity.getString(R.string.kind_passport_registration);
            case "passport_selfie":return activity.getString(R.string.kind_passport_selfie);
            case "identity_reference_selfie":return activity.getString(R.string.kind_identity_reference_selfie);
            case "driver_license_front":return activity.getString(R.string.kind_driver_license_front);
            case "driver_license_back":return activity.getString(R.string.kind_driver_license_back);
            case "sts_front":return activity.getString(R.string.kind_sts_front);
            case "sts_back":return activity.getString(R.string.kind_sts_back);
            case "rental_contract":return activity.getString(R.string.kind_rental_contract);
            case "osago":return activity.getString(R.string.kind_osago);
            case "taxi_permit":return activity.getString(R.string.kind_taxi_permit);
            case "diagnostic_card":return activity.getString(R.string.kind_diagnostic_card);
            case "left":return activity.getString(R.string.kind_left);case "front":return activity.getString(R.string.kind_front);
            case "right":return activity.getString(R.string.kind_right);case "rear":return activity.getString(R.string.kind_rear);
            case "open_trunk":return activity.getString(R.string.kind_open_trunk);
            case "rear_cabin":return activity.getString(R.string.kind_rear_cabin);case "front_cabin":return activity.getString(R.string.kind_front_cabin);
            default:return activity.getString(R.string.review_document_label);
        }
    }
    String status(String status){int id;
        switch(status){case "submitted":id=R.string.submitted;break;case "under_review":case "start":id=R.string.under_review;break;
            case "changes_required":id=R.string.changes_required;break;case "approved":id=R.string.status_approved;break;
            case "rejected":id=R.string.status_rejected;break;case "verified":id=R.string.status_verified;break;
            case "draft":id=R.string.status_draft;break;case "pending":case "pending_review":id=R.string.status_pending;break;
            case "retry_required":id=R.string.status_retry_required;break;case "identity_mismatch":id=R.string.status_identity_mismatch;break;
            case "manual_review_required":id=R.string.status_manual_review_required;break;
            case "photo_check_required":id=R.string.status_photo_check_required;break;case "suspended":id=R.string.status_suspended;break;
            default:id=R.string.status_unknown;
        }return activity.getString(id);
    }
}
