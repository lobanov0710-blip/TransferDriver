package ru.transferservis.admin.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.*;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import ru.transferservis.admin.R;
import ru.transferservis.admin.data.*;

/** Foreground metadata and explicit, confirmed photo-check commands. No material preview. */
public final class PhotoControlActivity extends AppCompatActivity {
    private PhotoControlRepository repository;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Deque<String> vehicleHistory=new ArrayDeque<>(),checkHistory=new ArrayDeque<>();
    private PhotoControl.Vehicles vehicles;private PhotoControl.Checks checks;private PhotoControl.Vehicle selected;
    private AdminAccess access;private PhotoCheckAction uncertain;private AlertDialog confirmation;
    private String status="",vehicleCursor="",checkCursor="";private long scope;private int generation,stage,message;
    private boolean started,busy;private static final int VEHICLES=0,CHECKS=1,FORM=2;
    @Override protected void onCreate(Bundle saved){
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);super.onCreate(saved);setContentView(R.layout.activity_photo_control);
        View root=findViewById(R.id.photoRoot);getWindow().getDecorView().setFilterTouchesWhenObscured(true);WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        boolean light=(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)!=android.content.res.Configuration.UI_MODE_NIGHT_YES;
        if(android.os.Build.VERSION.SDK_INT>=30){int mask=android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;getWindow().getInsetsController().setSystemBarsAppearance(light?mask:0,mask);}
        else{WindowInsetsControllerCompat bars=WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView());bars.setAppearanceLightStatusBars(light);bars.setAppearanceLightNavigationBars(light);}
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());int pad=dp(16);view.setPadding(pad+bars.left,bars.top,pad+bars.right,pad+Math.max(bars.bottom,insets.getInsets(WindowInsetsCompat.Type.ime()).bottom));return insets;});
        ViewCompat.requestApplyInsets(root);ViewCompat.setAccessibilityHeading(findViewById(R.id.photoHeading),true);
        repository=new PhotoControlRepository(this);((MaterialToolbar)findViewById(R.id.photoToolbar)).setNavigationOnClickListener(v->back());
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){@Override public void handleOnBackPressed(){back();}});
        spinner(R.id.photoKind,R.array.photo_kinds);spinner(R.id.photoMode,R.array.photo_modes);spinner(R.id.photoFilter,R.array.photo_filters);
        ((Spinner)findViewById(R.id.photoFilter)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> p,View v,int n,long id){String next=n==0?"":PhotoControl.STATUSES.get(n-1);if(started&&!busy&&uncertain==null&&!next.equals(status)){status=next;vehicleHistory.clear();loadVehicles("",0);}}
            @Override public void onNothingSelected(AdapterView<?> p){ }
        });
        findViewById(R.id.photoReload).setOnClickListener(v->{if(!busy){if(scope==0||!repository.current(scope))restart();else if(uncertain==null&&stage!=VEHICLES&&selected!=null)loadChecks(selected,checkCursor,0);else loadVehicles(vehicleCursor,0);}});
        findViewById(R.id.photoPrevious).setOnClickListener(v->page(false));findViewById(R.id.photoNext).setOnClickListener(v->page(true));
        findViewById(R.id.photoNew).setOnClickListener(v->{if(!busy&&checks!=null&&access!=null&&access.canManagePhotoChecks()&&checks.vehicle.canRequest()){stage=FORM;clearReason();message=0;render();}});
        findViewById(R.id.photoConfirm).setOnClickListener(v->prepare());findViewById(R.id.photoRetry).setOnClickListener(v->{if(!busy&&uncertain!=null)confirm(uncertain);});
        findViewById(R.id.photoRows).setSaveFromParentEnabled(false);render();
    }
    private void spinner(int id,int entries){((Spinner)findViewById(id)).setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,getResources().getStringArray(entries)));}
    @Override protected void onStart(){super.onStart();started=true;restart();}
    @Override protected void onStop(){started=false;generation++;repository.cancel();scope=0;busy=false;if(confirmation!=null){confirmation.dismiss();confirmation=null;}
        vehicles=null;checks=null;selected=null;access=null;uncertain=null;stage=VEHICLES;status="";vehicleCursor="";checkCursor="";vehicleHistory.clear();checkHistory.clear();message=0;
        ((Spinner)findViewById(R.id.photoFilter)).setSelection(0);clearReason();render();super.onStop();}
    @Override protected void onDestroy(){repository.cancel();executor.shutdownNow();super.onDestroy();}
    private void restart(){if(!started||busy)return;vehicles=null;checks=null;selected=null;uncertain=null;access=null;vehicleHistory.clear();checkHistory.clear();clearReason();
        try{scope=repository.bind();loadVehicles("",0);}catch(IOException failure){scope=0;message=error(failure);render();}}
    private interface Task<T>{PhotoControlRepository.Read<T> run(long expected)throws IOException;}
    private interface Accept<T>{void accept(T value);}
    private <T> void read(Task<T> task,Accept<T> accept){if(!started||busy||scope==0)return;int ticket=++generation;long expected=scope;busy=true;message=R.string.photo_loading;render();
        executor.execute(()->{try{PhotoControlRepository.Read<T> value=task.run(expected);runOnUiThread(()->{if(!active(ticket))return;if(!repository.current(expected)){failed(null,true);return;}
            busy=false;access=value.access;message=uncertain==null?0:R.string.photo_uncertain_checked;
            if(uncertain!=null&&!access.canManagePhotoChecks()){uncertain=null;message=R.string.photo_permission;}
            accept.accept(value.data);render();});}catch(IOException|RuntimeException failure){runOnUiThread(()->{if(active(ticket))failed(failure,!repository.current(expected));});}});}
    private void loadVehicles(String cursor,int direction){if(busy)return;stage=VEHICLES;selected=null;checks=null;clearReason();String prior=vehicleCursor;
        read(expected->repository.vehicles(expected,status,cursor),value->{if(direction>0)vehicleHistory.addLast(prior);else if(direction<0)vehicleHistory.removeLast();vehicleCursor=cursor;vehicles=value;});}
    private void loadChecks(PhotoControl.Vehicle vehicle,String cursor,int direction){if(busy||uncertain!=null)return;stage=CHECKS;selected=vehicle;clearReason();String prior=checkCursor;
        read(expected->repository.checks(expected,vehicle,cursor),value->{if(direction>0)checkHistory.addLast(prior);else if(direction<0)checkHistory.removeLast();checkCursor=cursor;checks=value;});}
    private void page(boolean next){if(busy||uncertain!=null)return;if(stage==VEHICLES&&vehicles!=null){String cursor=next?vehicles.nextCursor:vehicleHistory.peekLast();if(cursor!=null)loadVehicles(cursor,next?1:-1);}
        else if(stage==CHECKS&&checks!=null){String cursor=next?checks.nextCursor:checkHistory.peekLast();if(cursor!=null)loadChecks(checks.vehicle,cursor,next?1:-1);}}
    private void prepare(){if(busy||uncertain!=null||checks==null)return;
        int kind=((Spinner)findViewById(R.id.photoKind)).getSelectedItemPosition(),mode=((Spinner)findViewById(R.id.photoMode)).getSelectedItemPosition();
        try{PhotoCheckAction action=PhotoCheckAction.create(access,scope,checks,kind==1?"complaint":kind==2?"manual":"",mode==1?"after_current_trip":mode==2?"immediate":"",((EditText)findViewById(R.id.photoReason)).getText().toString());confirm(action);}
        catch(IOException invalid){((EditText)findViewById(R.id.photoReason)).setError(getString(R.string.photo_required));}}
    private void confirm(PhotoCheckAction action){if(busy||!started)return;if(!repository.current(action.scope)){failed(null,true);return;}
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(findViewById(R.id.photoReason).getWindowToken(),0);
        confirmation=new MaterialAlertDialogBuilder(this).setTitle(R.string.photo_confirm_title).setMessage(actionText(action)).setNegativeButton(R.string.photo_no,null).setPositiveButton(R.string.photo_yes,(dialog,which)->{
            confirmation=null;if(started&&!busy){if(!repository.current(action.scope)){failed(null,true);return;}clearReason();send(action);}}).create();
        if(confirmation.getWindow()==null){confirmation=null;return;}confirmation.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        confirmation.getWindow().getDecorView().setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);confirmation.show();confirmation.getButton(AlertDialog.BUTTON_POSITIVE).setFilterTouchesWhenObscured(true);
    }
    private void send(PhotoCheckAction action){if(!started||busy)return;int ticket=++generation;busy=true;vehicles=null;message=R.string.photo_saving;render();
        executor.execute(()->{boolean ack=false;try{repository.apply(action);ack=true;PhotoControlRepository.Read<PhotoControl.Checks> value=repository.checks(action.scope,action.vehicle.afterOwnMutation(),"");
            runOnUiThread(()->{if(!active(ticket))return;if(!repository.current(action.scope)){failed(null,true);return;}busy=false;uncertain=null;access=value.access;checks=value.data;selected=checks.vehicle;checkHistory.clear();checkCursor="";stage=CHECKS;message=R.string.photo_updated;render();});
        }catch(IOException|RuntimeException failure){boolean acknowledged=ack;runOnUiThread(()->{if(!active(ticket))return;if(!repository.current(action.scope)){failed(null,true);return;}busy=false;checks=null;selected=null;vehicles=null;stage=VEHICLES;
            boolean definitive=failure instanceof AdminApiClient.Failure&&((AdminApiClient.Failure)failure).status>=400&&((AdminApiClient.Failure)failure).status<500&&((AdminApiClient.Failure)failure).status!=408;
            if(acknowledged||definitive){uncertain=null;message=acknowledged?R.string.photo_ack_reload:error(failure);if(closesSelection(failure))access=null;}
            else{uncertain=action;message=R.string.photo_uncertain;}render();});}});
    }
    private boolean active(int ticket){return started&&ticket==generation&&!isFinishing();}
    private void failed(Throwable failure,boolean changed){busy=false;vehicles=null;checks=null;selected=null;stage=VEHICLES;clearReason();if(changed||closesSelection(failure)){uncertain=null;access=null;if(confirmation!=null){confirmation.dismiss();confirmation=null;}}
        if(changed){scope=0;repository.cancel();}message=changed?R.string.photo_session:error(failure);render();}
    private boolean closesSelection(Throwable f){if(!(f instanceof AdminApiClient.Failure))return false;int s=((AdminApiClient.Failure)f).status;return s==401||s==403||s==404||s==409;}
    private int error(Throwable f){if(f instanceof AdminApiClient.Failure){switch(((AdminApiClient.Failure)f).status){case 401:return R.string.photo_session;case 403:return R.string.photo_permission;case 404:return R.string.photo_not_found;case 409:return R.string.photo_conflict;case 422:return R.string.photo_blocked;default:break;}}return R.string.photo_failed;}
    private void back(){if(busy||uncertain!=null||stage==VEHICLES){finish();return;}if(stage==FORM&&selected!=null){loadChecks(selected,checkCursor,0);return;}loadVehicles(vehicleCursor,0);}
    private void clearReason(){((EditText)findViewById(R.id.photoReason)).setText("");((EditText)findViewById(R.id.photoReason)).setError(null);((Spinner)findViewById(R.id.photoKind)).setSelection(0);((Spinner)findViewById(R.id.photoMode)).setSelection(0);}
    private void render(){show(R.id.photoProgress,busy);show(R.id.photoFilter,!busy&&stage==VEHICLES&&uncertain==null);
        ((TextView)findViewById(R.id.photoMessage)).setText(message==0?"":getString(message));((TextView)findViewById(R.id.photoHeading)).setText(uncertain!=null?R.string.photo_uncertain_title:stage==VEHICLES?R.string.photo_choose_vehicle:stage==CHECKS?R.string.photo_checks_title:R.string.photo_form_title);
        LinearLayout rows=findViewById(R.id.photoRows);rows.removeAllViews();rows.setVisibility(busy?View.GONE:View.VISIBLE);
        show(R.id.photoForm,!busy&&uncertain==null&&stage==FORM&&checks!=null);show(R.id.photoConfirm,!busy&&uncertain==null&&stage==FORM&&checks!=null);
        show(R.id.photoNew,!busy&&uncertain==null&&stage==CHECKS&&checks!=null&&checks.vehicle.canRequest()&&access!=null&&access.canManagePhotoChecks());show(R.id.photoRetry,!busy&&uncertain!=null);findViewById(R.id.photoReload).setEnabled(!busy);
        show(R.id.photoPaging,!busy&&uncertain==null&&((stage==VEHICLES&&vehicles!=null)||(stage==CHECKS&&checks!=null)));
        findViewById(R.id.photoPrevious).setEnabled(stage==VEHICLES?!vehicleHistory.isEmpty():!checkHistory.isEmpty());String next=stage==VEHICLES?(vehicles==null?null:vehicles.nextCursor):(checks==null?null:checks.nextCursor);findViewById(R.id.photoNext).setEnabled(next!=null);
        ((TextView)findViewById(R.id.photoSummary)).setText(stage==FORM&&selected!=null?vehicleText(selected):"");if(busy)return;
        if(uncertain!=null){label(rows,getString(R.string.photo_original)+"\n"+actionText(uncertain));if(vehicles!=null)for(PhotoControl.Vehicle v:vehicles.rows)if(v.id.equals(uncertain.vehicle.id))label(rows,vehicleText(v));return;}
        if(stage==VEHICLES&&vehicles!=null){if(vehicles.rows.isEmpty())label(rows,getString(R.string.photo_empty_vehicles));for(PhotoControl.Vehicle v:vehicles.rows)row(rows,vehicleText(v),access!=null&&access.canManagePhotoChecks(),()->{checkHistory.clear();checkCursor="";loadChecks(v,"",0);});}
        else if(stage==CHECKS&&checks!=null){label(rows,vehicleText(checks.vehicle));if(checks.rows.isEmpty())label(rows,getString(R.string.photo_empty_checks));for(PhotoControl.Check c:checks.rows)label(rows,getString(R.string.photo_check_row,
            map(c.kind,new String[]{"monthly","complaint","manual"},R.array.photo_check_kinds),map(c.result,new String[]{"pending","passed","failed","manual_review"},R.array.photo_results),
            map(c.restrictionMode,new String[]{"grace_period","after_current_trip","immediate"},R.array.photo_check_modes),date(c.dueAt),date(c.graceUntil),c.id));}
    }
    private String vehicleText(PhotoControl.Vehicle v){return getString(R.string.photo_vehicle_row,v.make,v.model,v.plate,getResources().getStringArray(R.array.photo_filters)[PhotoControl.STATUSES.indexOf(v.status)+1],date(v.lastPassedAt),date(v.nextDueAt),v.id);}
    private String actionText(PhotoCheckAction a){return getString(R.string.photo_confirm_body,vehicleText(a.vehicle),getResources().getStringArray(R.array.photo_kinds)["complaint".equals(a.kind)?1:2],getResources().getStringArray(R.array.photo_modes)["after_current_trip".equals(a.mode)?1:2],a.reason);}
    private String date(Long time){return time==null?getString(R.string.photo_unknown):java.time.Instant.ofEpochMilli(time).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm",Locale.getDefault()));}
    private String map(String key,String[] values,int resource){return getResources().getStringArray(resource)[Arrays.asList(values).indexOf(key)];}
    private void row(LinearLayout rows,String text,boolean enabled,Runnable action){MaterialButton b=new MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);b.setText(text);b.setAllCaps(false);b.setSingleLine(false);b.setMaxLines(Integer.MAX_VALUE);b.setEllipsize(null);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setMinHeight(dp(48));b.setSaveEnabled(false);b.setFilterTouchesWhenObscured(true);b.setEnabled(enabled);b.setOnClickListener(v->action.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);rows.addView(b,p);}
    private void label(LinearLayout rows,String text){TextView v=new TextView(this);v.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);v.setText(text);v.setSaveEnabled(false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(8);p.bottomMargin=dp(8);rows.addView(v,p);}
    private void show(int id,boolean visible){findViewById(id).setVisibility(visible?View.VISIBLE:View.GONE);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
