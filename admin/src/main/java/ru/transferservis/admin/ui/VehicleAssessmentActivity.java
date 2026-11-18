package ru.transferservis.admin.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.*;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import ru.transferservis.admin.R;
import ru.transferservis.admin.data.*;

/** Foreground-only review. Server computes class; unknown writes retain only their original intent. */
public final class VehicleAssessmentActivity extends AppCompatActivity {
    private VehicleAssessmentRepository repository;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final List<Spinner> decisions=new ArrayList<>();private final List<CheckBox> waivers=new ArrayList<>();
    private final List<VehicleAssessment.Document> fronts=new ArrayList<>(),backs=new ArrayList<>();
    private String account,driver,vehicle;private long scope;private int generation;private boolean started,busy;
    private AdminAccess access;private VehicleAssessment.Context context;private VehicleAssessment.Selection selection;
    private VehicleAssessmentAction uncertain;private AlertDialog confirmation;private Bitmap preview;
    private static final String[] POWERS={"","combustion","hybrid","battery_electric"},BODIES={"","sedan","crossover","minivan","other"},RESULTS={"","satisfied","unmet"};
    public static Intent intent(Context c,String account,String driver,String vehicle){return new Intent(c,VehicleAssessmentActivity.class).putExtra("account",account).putExtra("driver",driver).putExtra("vehicle",vehicle);}
    @Override protected void onCreate(Bundle saved){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);super.onCreate(null);setContentView(R.layout.activity_vehicle_assessment);
        account=getIntent().getStringExtra("account");driver=getIntent().getStringExtra("driver");vehicle=getIntent().getStringExtra("vehicle");
        for(String key:new String[]{"account","driver","vehicle"})getIntent().removeExtra(key);
        if(saved!=null||!id(account,"staff")||!id(driver,"staff")||!id(vehicle,"vehicle")){finish();return;}
        View root=findViewById(R.id.assessmentRoot);root.setSaveFromParentEnabled(false);getWindow().getDecorView().setFilterTouchesWhenObscured(true);
        if(Build.VERSION.SDK_INT>=30)root.setImportantForContentCapture(View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);boolean light=(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)!=android.content.res.Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat bars=WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView());bars.setAppearanceLightStatusBars(light);bars.setAppearanceLightNavigationBars(light);
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{Insets b=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());int p=Math.round(16*getResources().getDisplayMetrics().density);v.setPadding(p+b.left,b.top,p+b.right,p+b.bottom);return insets;});ViewCompat.requestApplyInsets(root);
        repository=new VehicleAssessmentRepository(this);((MaterialToolbar)findViewById(R.id.assessmentToolbar)).setNavigationOnClickListener(v->finish());
        adapter(spinner(R.id.assessmentPower),R.array.assessment_powers);adapter(spinner(R.id.assessmentBody),R.array.assessment_bodies);
        for(int i:new int[]{R.id.assessmentRuleLoad,R.id.assessmentConfirm,R.id.assessmentRetry,R.id.assessmentReload,R.id.assessmentPreviewFront,R.id.assessmentPreviewBack,R.id.assessmentException,R.id.assessmentWaiveYear})findViewById(i).setFilterTouchesWhenObscured(true);
        check(R.id.assessmentException).setOnCheckedChangeListener((b,value)->{selection=null;clearConditions();text(R.id.assessmentRule,"");render();});
        findViewById(R.id.assessmentReload).setOnClickListener(v->{if(!busy)reload();});
        findViewById(R.id.assessmentRuleLoad).setOnClickListener(v->loadRule());
        findViewById(R.id.assessmentConfirm).setOnClickListener(v->prepare());
        findViewById(R.id.assessmentRetry).setOnClickListener(v->{if(!busy&&uncertain!=null)confirm(uncertain);});
        findViewById(R.id.assessmentPreviewFront).setOnClickListener(v->preview(document(true)));
        findViewById(R.id.assessmentPreviewBack).setOnClickListener(v->preview(document(false)));render();
    }
    private static boolean id(String value,String prefix){return value!=null&&value.matches(prefix+"_[A-Za-z0-9_-]{8,100}");}
    @Override protected void onStart(){super.onStart();started=true;if(repository!=null)reload();}
    @Override protected void onStop(){started=false;generation++;handler.removeCallbacksAndMessages(null);if(repository!=null)repository.cancel();scope=0;busy=false;uncertain=null;access=null;context=null;wipe();message(0);render();super.onStop();}
    @Override protected void onDestroy(){if(repository!=null)repository.cancel();executor.shutdownNow();handler.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.clear();}
    private boolean active(int ticket){return started&&ticket==generation&&!isFinishing();}
    private void reload(){if(!started||busy)return;wipe();context=null;access=null;message(R.string.assessment_loading);busy=true;render();int ticket=++generation;
        try{if(uncertain==null)scope=repository.bind(account);else if(!repository.current(scope))throw new IOException();}
        catch(IOException e){fail(e,false);return;}long expected=scope;
        executor.execute(()->{try{VehicleAssessmentRepository.Read<VehicleAssessment.Context> r=repository.load(expected,driver,vehicle);runOnUiThread(()->{if(!active(ticket))return;if(!repository.current(expected)){fail(new IOException(),false);return;}busy=false;access=r.access;context=r.data;armMfa();
            if(uncertain==null)populate();else{text(R.id.assessmentSummary,summary(uncertain));message(R.string.assessment_uncertain);}render();});
        }catch(IOException|RuntimeException e){runOnUiThread(()->{if(active(ticket))fail(e,false);});}});
    }
    private void populate(){VehicleClassification.Parameters p=context.page.declared;text(R.id.assessmentMake,p.make);text(R.id.assessmentModel,p.model);text(R.id.assessmentYear,p.year==null?"":p.year.toString());
        text(R.id.assessmentSummary,getString(R.string.assessment_snapshot,p.make,p.model,context.plate,p.year==null?getString(R.string.classification_unknown):p.year.toString()));
        VehicleClassification.Parameters verified=context.page.verified;if(verified!=null){spinner(R.id.assessmentPower).setSelection(Arrays.asList(POWERS).indexOf(verified.powertrain));spinner(R.id.assessmentBody).setSelection(Arrays.asList(BODIES).indexOf(verified.bodyType));}
        documents(true);documents(false);message(0);
    }
    private void documents(boolean front){List<VehicleAssessment.Document> target=front?fronts:backs;target.clear();List<String> names=new ArrayList<>();names.add(getString(R.string.assessment_select_document));for(VehicleAssessment.Document d:context.documents)if((front?"sts_front":"sts_back").equals(d.kind)){target.add(d);names.add(getString(R.string.assessment_document_row,d.id,d.revision));}spinner(front?R.id.assessmentFront:R.id.assessmentBack).setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));}
    private VehicleAssessment.Document document(boolean front){List<VehicleAssessment.Document> list=front?fronts:backs;int n=spinner(front?R.id.assessmentFront:R.id.assessmentBack).getSelectedItemPosition()-1;return n>=0&&n<list.size()?list.get(n):null;}
    private VehicleAssessment.Parameters parameters()throws IOException{return new VehicleAssessment.Parameters(value(R.id.assessmentMake),value(R.id.assessmentModel),value(R.id.assessmentYear),POWERS[spinner(R.id.assessmentPower).getSelectedItemPosition()],BODIES[spinner(R.id.assessmentBody).getSelectedItemPosition()]);}
    private void loadRule(){if(busy||context==null||uncertain!=null)return;try{VehicleAssessment.Parameters p=parameters();boolean exception=check(R.id.assessmentException).isChecked();String basis=exception?VehicleAssessment.text(value(R.id.assessmentBasis),80):"";VehicleAssessment.Context c=context;
        busy=true;selection=null;clearConditions();clearPreview();text(R.id.assessmentRule,"");message(R.string.assessment_loading);render();int ticket=++generation;
        executor.execute(()->{try{VehicleAssessmentRepository.Read<VehicleAssessment.Selection> r=repository.select(c.scope,c,p,exception,basis);runOnUiThread(()->{if(!active(ticket))return;if(!repository.current(scope)){fail(new IOException(),false);return;}busy=false;access=r.access;selection=r.data;armMfa();
            VehicleClassification.Rule rule=selection.rule;text(R.id.assessmentRule,selection.targetPinned==null&&!exception?getString(R.string.assessment_no_rule):rule==null?getString(R.string.assessment_pinned,selection.targetPinned):getString(R.string.assessment_rule_row,rule.version,rule.make,rule.model,className(rule.vehicleClass)));
            for(VehicleClassification.Condition condition:selection.conditions){LinearLayout rows=findViewById(R.id.assessmentConditions);TextView title=new TextView(this);title.setText(condition.text);title.setSaveEnabled(false);rows.addView(title);Spinner choice=new Spinner(this);choice.setSaveEnabled(false);choice.setFilterTouchesWhenObscured(true);choice.setContentDescription(condition.text);adapter(choice,R.array.assessment_decisions);rows.addView(choice);decisions.add(choice);CheckBox waive=new CheckBox(this);waive.setText(R.string.assessment_waive_condition);waive.setSaveEnabled(false);waive.setFilterTouchesWhenObscured(true);waive.setVisibility(exception?View.VISIBLE:View.GONE);rows.addView(waive);waivers.add(waive);}
            message(0);render();});}catch(IOException|RuntimeException e){runOnUiThread(()->{if(active(ticket))fail(e,false);});}});
        }catch(IOException|RuntimeException e){message(R.string.assessment_required);}}
    private void prepare(){if(busy||context==null||uncertain!=null)return;try{List<String> results=new ArrayList<>();Set<String> waived=new HashSet<>();for(int i=0;i<decisions.size();i++){results.add(RESULTS[decisions.get(i).getSelectedItemPosition()]);if(waivers.get(i).isChecked())waived.add(selection.conditions.get(i).id);}boolean exception=check(R.id.assessmentException).isChecked();
        VehicleAssessmentAction action=VehicleAssessmentAction.create(access,selection,parameters(),exception,exception?VehicleAssessment.text(value(R.id.assessmentBasis),80):"",document(true),document(false),results,waived,check(R.id.assessmentWaiveYear).isChecked(),value(R.id.assessmentReason));confirm(action);
        }catch(IOException|RuntimeException e){message(R.string.assessment_required);}}
    private void confirm(VehicleAssessmentAction action){if(!started||busy||!repository.current(action.scope)){fail(new IOException(),false);return;}dismiss();confirmation=new AlertDialog.Builder(this).setTitle(R.string.assessment_confirm_title).setMessage(summary(action)).setNegativeButton(R.string.assessment_no,null).setPositiveButton(R.string.assessment_yes,(d,w)->send(action)).create();if(confirmation.getWindow()!=null)confirmation.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);confirmation.show();confirmation.getButton(AlertDialog.BUTTON_POSITIVE).setFilterTouchesWhenObscured(true);}
    private String summary(VehicleAssessmentAction a){VehicleAssessment.Parameters p=a.selection.parameters;JsonObject b=a.body();StringBuilder conditions=new StringBuilder();JsonArray waived=a.selection.exception?b.getAsJsonObject("exception").getAsJsonArray("waivedConditionIds"):new JsonArray();for(VehicleClassification.Condition c:a.selection.conditions){String result=getResources().getStringArray(R.array.assessment_decisions)[0];for(JsonElement e:b.getAsJsonArray("conditions"))if(c.id.equals(e.getAsJsonObject().get("conditionId").getAsString()))result=getResources().getStringArray(R.array.assessment_decisions)["satisfied".equals(e.getAsJsonObject().get("result").getAsString())?1:2];conditions.append(getString(R.string.assessment_conditions_summary,c.text,result,waived.contains(new JsonPrimitive(c.id))?getString(R.string.assessment_waived_suffix):""));}
        return getString(R.string.assessment_confirm_body,a.context.plate,p.make,p.model,p.year,getResources().getStringArray(R.array.assessment_powers)[Arrays.asList(POWERS).indexOf(p.powertrain)],getResources().getStringArray(R.array.assessment_bodies)[Arrays.asList(BODIES).indexOf(p.bodyType)],getString(a.selection.exception?R.string.assessment_exception_mode:R.string.assessment_standard),a.selection.exception?a.selection.basisModel:getString(R.string.assessment_none),conditions.length()==0?getString(R.string.assessment_none):conditions.toString(),getString(a.selection.exception&&b.getAsJsonObject("exception").get("yearBelowMinimum").getAsBoolean()?R.string.assessment_yes:R.string.assessment_none),a.reason)+"\n"+getString(R.string.assessment_evidence_summary,documentSummary(b.getAsJsonArray("evidence")));}
    private String documentSummary(JsonArray docs){StringBuilder result=new StringBuilder();for(JsonElement item:docs){JsonObject d=item.getAsJsonObject();if(result.length()>0)result.append("; ");result.append(getString(R.string.assessment_document_row,d.get("documentId").getAsString(),d.get("expectedRevision").getAsLong()));}return result.toString();}
    private void send(VehicleAssessmentAction action){if(!started||busy)return;dismiss();uncertain=action;wipe();busy=true;message(R.string.assessment_saving);render();int ticket=++generation;executor.execute(()->{try{VehicleAssessmentAction.Ack ack=repository.apply(action);runOnUiThread(()->{if(!active(ticket))return;if(!repository.current(action.scope)){fail(new IOException(),true);return;}busy=false;uncertain=null;context=null;wipe();text(R.id.assessmentSummary,getString(R.string.assessment_recorded,"candidate".equals(ack.decision.outcome)?className(ack.decision.vehicleClass):getString("manual_review".equals(ack.decision.outcome)?R.string.classification_manual:R.string.classification_excluded))+(ack.dispatcher?"\n"+getString(R.string.assessment_dispatcher):""));message(0);render();});}catch(IOException|RuntimeException e){runOnUiThread(()->{if(active(ticket))fail(e,true);});}});}
    private void preview(VehicleAssessment.Document d){if(busy||context==null||uncertain!=null||d==null)return;VehicleAssessment.Context c=context;clearPreview();busy=true;message(R.string.assessment_loading);render();int ticket=++generation;executor.execute(()->{Bitmap decoded=null;try{byte[] bytes=repository.preview(c.scope,c,d);try{decoded=ReviewScreen.decode(bytes);}finally{Arrays.fill(bytes,(byte)0);}Bitmap result=decoded;runOnUiThread(()->{if(!active(ticket)||!repository.current(c.scope)){result.recycle();if(active(ticket))fail(new IOException(),false);return;}busy=false;preview=result;((ImageView)findViewById(R.id.assessmentPreview)).setImageBitmap(result);findViewById(R.id.assessmentPreview).setVisibility(View.VISIBLE);message(0);render();});}catch(IOException|RuntimeException e){if(decoded!=null)decoded.recycle();runOnUiThread(()->{if(active(ticket))fail(e,false);});}});}
    private void armMfa(){handler.removeCallbacksAndMessages(null);if(access!=null&&access.isOwner())handler.postDelayed(()->{if(started){generation++;repository.cancel();scope=0;busy=false;uncertain=null;context=null;access=null;wipe();message(R.string.assessment_permission);render();}},Math.max(1,access.mfaExpiresAt-System.currentTimeMillis()));}
    private void fail(Throwable e,boolean write){busy=false;clearPreview();int status=e instanceof AdminApiClient.Failure?((AdminApiClient.Failure)e).status:0;
        if(scope==0||!repository.current(scope)||status==401||status==403){uncertain=null;context=null;access=null;wipe();message(R.string.assessment_permission);}
        else if(write&&!(status>=400&&status<500&&status!=408)){wipe();text(R.id.assessmentSummary,summary(uncertain));message(R.string.assessment_uncertain);}
        else{if(write)uncertain=null;context=null;wipe();if(uncertain!=null)text(R.id.assessmentSummary,summary(uncertain));message(status==409?R.string.assessment_conflict:R.string.assessment_failed);}render();}
    private void render(){if(findViewById(R.id.assessmentRoot)==null)return;boolean form=started&&!busy&&context!=null&&uncertain==null;findViewById(R.id.assessmentProgress).setVisibility(busy?View.VISIBLE:View.GONE);findViewById(R.id.assessmentForm).setVisibility(form?View.VISIBLE:View.GONE);findViewById(R.id.assessmentConfirm).setEnabled(form);findViewById(R.id.assessmentRuleLoad).setEnabled(form);findViewById(R.id.assessmentReload).setEnabled(started&&!busy);findViewById(R.id.assessmentRetry).setVisibility(started&&!busy&&uncertain!=null?View.VISIBLE:View.GONE);
        boolean owner=access!=null&&access.canApproveVehicleException();findViewById(R.id.assessmentException).setVisibility(owner?View.VISIBLE:View.GONE);boolean exception=owner&&check(R.id.assessmentException).isChecked();findViewById(R.id.assessmentBasis).setVisibility(exception?View.VISIBLE:View.GONE);findViewById(R.id.assessmentWaiveYear).setVisibility(exception?View.VISIBLE:View.GONE);if(!exception)check(R.id.assessmentWaiveYear).setChecked(false);}
    private void wipe(){dismiss();selection=null;clearConditions();clearPreview();for(int id:new int[]{R.id.assessmentMake,R.id.assessmentModel,R.id.assessmentYear,R.id.assessmentBasis,R.id.assessmentReason,R.id.assessmentRule,R.id.assessmentSummary})text(id,"");check(R.id.assessmentException).setChecked(false);check(R.id.assessmentWaiveYear).setChecked(false);spinner(R.id.assessmentPower).setSelection(0);spinner(R.id.assessmentBody).setSelection(0);fronts.clear();backs.clear();spinner(R.id.assessmentFront).setAdapter(null);spinner(R.id.assessmentBack).setAdapter(null);}
    private void clearConditions(){decisions.clear();waivers.clear();((LinearLayout)findViewById(R.id.assessmentConditions)).removeAllViews();}
    private void clearPreview(){((ImageView)findViewById(R.id.assessmentPreview)).setImageDrawable(null);findViewById(R.id.assessmentPreview).setVisibility(View.GONE);if(preview!=null){preview.recycle();preview=null;}}
    private void dismiss(){if(confirmation!=null){confirmation.dismiss();confirmation=null;}}
    private void adapter(Spinner v,int array){ArrayAdapter<CharSequence> a=ArrayAdapter.createFromResource(this,array,android.R.layout.simple_spinner_dropdown_item);v.setAdapter(a);}
    private Spinner spinner(int id){return findViewById(id);}private CheckBox check(int id){return findViewById(id);}private String value(int id){return ((TextView)findViewById(id)).getText().toString();}private void text(int id,String value){((TextView)findViewById(id)).setText(value);}private void message(int id){text(R.id.assessmentMessage,id==0?"":getString(id));}
    private String className(String value){return getString("comfort".equals(value)?R.string.classification_comfort:"business".equals(value)?R.string.classification_business:R.string.classification_minivan);}
}
