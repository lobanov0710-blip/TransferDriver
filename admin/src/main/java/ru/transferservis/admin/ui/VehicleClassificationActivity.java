package ru.transferservis.admin.ui;

import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.*;
import com.google.android.material.appbar.MaterialToolbar;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import ru.transferservis.admin.R;
import ru.transferservis.admin.data.*;

/** Current verification and history only. No class picker, document content or mutation. */
public final class VehicleClassificationActivity extends AppCompatActivity {
    private VehicleClassificationRepository repository;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Deque<String> history=new ArrayDeque<>();
    private String account,vehicle,cursor="";private Long revision;
    private VehicleClassificationRepository.Read data;private long scope;private int generation;private boolean started,busy;
    public static Intent intent(Context context,String account,String vehicle){return new Intent(context,VehicleClassificationActivity.class).putExtra("account",account).putExtra("vehicle",vehicle);}
    @Override protected void onCreate(Bundle saved){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);super.onCreate(null);setContentView(R.layout.activity_vehicle_classification);
        account=getIntent().getStringExtra("account");vehicle=getIntent().getStringExtra("vehicle");getIntent().removeExtra("account");getIntent().removeExtra("vehicle");
        if(saved!=null||account==null||!account.matches("staff_[A-Za-z0-9_-]{8,94}")||vehicle==null||!vehicle.matches("vehicle_[A-Za-z0-9_-]{8,100}")){finish();return;}
        View root=findViewById(R.id.classificationRoot);root.setSaveFromParentEnabled(false);getWindow().getDecorView().setFilterTouchesWhenObscured(true);
        // FLAG_SECURE protects the entire window, including API29 content capture.
        // The additional View-level opt-out is available starting at API30.
        if(Build.VERSION.SDK_INT>=30)root.setImportantForContentCapture(View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        boolean light=(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)!=android.content.res.Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat bars=WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView());bars.setAppearanceLightStatusBars(light);bars.setAppearanceLightNavigationBars(light);
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{Insets b=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());int p=dp(16);v.setPadding(p+b.left,b.top,p+b.right,p+b.bottom);return insets;});ViewCompat.requestApplyInsets(root);
        repository=new VehicleClassificationRepository(this);
        ((MaterialToolbar)findViewById(R.id.classificationToolbar)).setNavigationOnClickListener(v->finish());
        for(int id:new int[]{R.id.classificationReload,R.id.classificationPrevious,R.id.classificationNext})findViewById(id).setFilterTouchesWhenObscured(true);
        findViewById(R.id.classificationReload).setOnClickListener(v->{if(!busy)restart();});
        findViewById(R.id.classificationPrevious).setOnClickListener(v->{if(!busy&&!history.isEmpty())load(history.peekLast(),-1);});
        findViewById(R.id.classificationNext).setOnClickListener(v->{if(!busy&&data!=null&&data.page.nextCursor!=null)load(data.page.nextCursor,1);});
        for(int id:new int[]{R.id.classificationDeclared,R.id.classificationVerified,R.id.classificationResult,R.id.classificationRule,R.id.classificationConditions,R.id.classificationHistory})ViewCompat.setAccessibilityHeading(findViewById(id),true);
        render();
    }
    @Override protected void onStart(){super.onStart();started=true;if(repository!=null)restart();}
    @Override protected void onStop(){started=false;generation++;handler.removeCallbacksAndMessages(null);if(repository!=null)repository.cancel();scope=0;busy=false;data=null;revision=null;cursor="";history.clear();clear();render();super.onStop();}
    @Override protected void onDestroy(){if(repository!=null)repository.cancel();executor.shutdownNow();handler.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.clear();}
    private void restart(){if(!started||busy)return;data=null;revision=null;cursor="";history.clear();handler.removeCallbacksAndMessages(null);
        try{scope=repository.bind(account);load("",0);}catch(IOException e){fail(e);}}
    private void load(String next,int direction){if(!started||busy||scope==0)return;int ticket=++generation;long expected=scope;Long expectedRevision=revision;String previous=cursor;
        busy=true;data=null;clear();message(R.string.classification_loading);render();
        executor.execute(()->{try{VehicleClassificationRepository.Read result=repository.read(expected,vehicle,next,expectedRevision);
            runOnUiThread(()->{if(!active(ticket))return;if(!repository.current(expected)){fail(new IOException());return;}
                if(!result.access.canReadClassification()){fail(new IOException());return;}
                if(result.page.nextCursor!=null&&history.contains(result.page.nextCursor)){fail(new IOException());return;}
                busy=false;data=result;revision=result.page.revision;if(direction>0)history.addLast(previous);else if(direction<0)history.removeLast();cursor=next;
                message(0);render();((ScrollView)findViewById(R.id.classificationScroll)).scrollTo(0,0);handler.removeCallbacksAndMessages(null);
                if(result.access.isOwner())handler.postDelayed(()->{if(started&&data==result){repository.cancel();scope=0;fail(new IOException());}},Math.max(1,result.access.mfaExpiresAt-System.currentTimeMillis()));
            });
        }catch(IOException|RuntimeException e){runOnUiThread(()->{if(active(ticket))fail(e);});}});
    }
    private boolean active(int ticket){return started&&generation==ticket&&!isFinishing();}
    private void fail(Throwable failure){busy=false;data=null;revision=null;cursor="";history.clear();handler.removeCallbacksAndMessages(null);clear();int text=R.string.classification_failed;
        if(repository==null||scope==0||!repository.current(scope))text=R.string.classification_session;
        else if(failure instanceof AdminApiClient.Failure){switch(((AdminApiClient.Failure)failure).status){case 401:text=R.string.classification_session;break;case 403:text=R.string.classification_permission;break;case 404:text=R.string.classification_missing;break;case 409:text=R.string.classification_stale;break;default:break;}}
        message(text);render();
    }
    private void clear(){for(int id:new int[]{R.id.classificationDeclared,R.id.classificationVerified,R.id.classificationResult,R.id.classificationRule,R.id.classificationEvidence,R.id.classificationConditions,R.id.classificationHistory,R.id.classificationMessage})text(id,"");}
    private void render(){if(findViewById(R.id.classificationRoot)==null)return;
        findViewById(R.id.classificationProgress).setVisibility(busy?View.VISIBLE:View.GONE);findViewById(R.id.classificationReload).setEnabled(!busy&&started);
        findViewById(R.id.classificationRows).setVisibility(data!=null&&!busy?View.VISIBLE:View.GONE);findViewById(R.id.classificationPaging).setVisibility(data!=null&&!busy?View.VISIBLE:View.GONE);
        findViewById(R.id.classificationPrevious).setEnabled(!history.isEmpty());findViewById(R.id.classificationNext).setEnabled(data!=null&&data.page.nextCursor!=null);
        if(data==null||busy)return;VehicleClassification.Page p=data.page;VehicleClassification.Parameters d=p.declared,v=p.verified;
        text(R.id.classificationDeclared,getString(R.string.classification_declared,d.make,d.model,d.year==null?unknown():d.year.toString(),blank(d.color)?unknown():d.color));
        text(R.id.classificationVerified,v==null?getString(R.string.classification_unverified):getString(R.string.classification_verified,v.make,v.model,v.year.toString(),power(v.powertrain),body(v.bodyType)));
        String result=p.decision==null?getString(R.string.classification_no_class):"candidate".equals(p.decision.outcome)?getString(R.string.classification_candidate,className(p.decision.vehicleClass)):getString("manual_review".equals(p.decision.outcome)?R.string.classification_manual:R.string.classification_excluded);
        if(p.decision!=null)for(String reason:p.decision.reasons)result+="\n"+getString(reason(reason));text(R.id.classificationResult,result);
        String model=data.rule==null?getString(p.decision==null||p.decision.ruleVersion==null?R.string.classification_rule_absent:R.string.classification_rule_unavailable):getString(R.string.classification_rule_model,data.rule.make,data.rule.model,className(data.rule.vehicleClass),data.rule.minYear);
        text(R.id.classificationRule,getString(R.string.classification_rule,p.pinnedRuleVersion==null?unknown():p.pinnedRuleVersion,p.decision==null||p.decision.ruleVersion==null?unknown():p.decision.ruleVersion,model));
        text(R.id.classificationEvidence,getString(R.string.classification_documents,p.evidence.size()));
        StringBuilder conditions=new StringBuilder(getString(R.string.classification_conditions));if(p.conditions.isEmpty())conditions.append('\n').append(getString(R.string.classification_no_conditions));for(VehicleClassification.Condition c:p.conditions)conditions.append("\n• ").append(c.text);text(R.id.classificationConditions,conditions.toString());
        StringBuilder records=new StringBuilder(getString(R.string.classification_history)).append("\n\n");if(p.history.isEmpty())records.append(getString(R.string.classification_no_history));
        for(VehicleClassification.History h:p.history)records.append(getString(R.string.classification_history_row,Instant.ofEpochMilli(h.createdAt).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm",Locale.getDefault())),getString("candidate".equals(h.outcome)?R.string.classification_history_candidate:"manual_review".equals(h.outcome)?R.string.classification_history_manual:R.string.classification_history_excluded),h.ruleVersion==null?unknown():h.ruleVersion));
        text(R.id.classificationHistory,records.toString());
    }
    private String unknown(){return getString(R.string.classification_unknown);}
    private boolean blank(String value){if(value==null)return true;for(int i=0;i<value.length();i++){char c=value.charAt(i);if(!Character.isWhitespace(c)&&!Character.isSpaceChar(c)&&c!='\uFEFF')return false;}return true;}
    private String className(String c){return getString("comfort".equals(c)?R.string.classification_comfort:"business".equals(c)?R.string.classification_business:R.string.classification_minivan);}
    private String power(String c){return getString("combustion".equals(c)?R.string.classification_combustion:"hybrid".equals(c)?R.string.classification_hybrid:R.string.classification_bev);}
    private String body(String c){return getString("sedan".equals(c)?R.string.classification_sedan:"crossover".equals(c)?R.string.classification_crossover:"minivan".equals(c)?R.string.classification_minivan:R.string.classification_other);}
    private int reason(String c){switch(c){case "battery_electric":return R.string.classification_reason_bev;case "model_not_found":return R.string.classification_reason_model;case "body_type_not_allowed":return R.string.classification_reason_body;case "year_below_minimum":return R.string.classification_reason_year;case "rule_conditions_unverified":return R.string.classification_reason_unverified;case "rule_conditions_unmet":return R.string.classification_reason_unmet;default:return R.string.classification_reason_other;}}
    private void text(int id,String text){TextView v=findViewById(id);if(v!=null)v.setText(text);}
    private void message(int id){text(R.id.classificationMessage,id==0?"":getString(id));}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
