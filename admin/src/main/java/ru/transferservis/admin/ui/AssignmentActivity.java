package ru.transferservis.admin.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.appbar.MaterialToolbar;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ru.transferservis.admin.R;
import ru.transferservis.admin.data.AdminAccess;
import ru.transferservis.admin.data.AdminApiClient;
import ru.transferservis.admin.data.AssignmentAction;
import ru.transferservis.admin.data.AssignmentRepository;
import ru.transferservis.admin.data.Assignments;

/** Foreground-only operational surface. A human confirms every immutable assignment intent. */
public final class AssignmentActivity extends AppCompatActivity {
    private AssignmentRepository repository;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Deque<String> orderHistory=new ArrayDeque<>(),optionHistory=new ArrayDeque<>();
    private Assignments.Orders orders;
    private Assignments.Options options;
    private Assignments.Option selected;
    private AdminAccess access;
    private AssignmentAction uncertain;
    private AlertDialog confirmation;
    private String status="",orderCursor="",optionCursor="";
    private long scope;
    private int generation,stage,message;
    private boolean started,busy;
    private static final int ORDERS=0,OPTIONS=1,FORM=2;
    @Override protected void onCreate(Bundle saved) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);super.onCreate(saved);setContentView(R.layout.activity_assignment);
        View root=findViewById(R.id.assignmentRoot);getWindow().getDecorView().setFilterTouchesWhenObscured(true);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        boolean light=(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)!=android.content.res.Configuration.UI_MODE_NIGHT_YES;
        if(android.os.Build.VERSION.SDK_INT>=30){int mask=android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            getWindow().getInsetsController().setSystemBarsAppearance(light?mask:0,mask);
        }else{androidx.core.view.WindowInsetsControllerCompat bars=WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView());bars.setAppearanceLightStatusBars(light);bars.setAppearanceLightNavigationBars(light);}
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());int pad=dp(16);
            view.setPadding(pad+bars.left,bars.top,pad+bars.right,pad+Math.max(bars.bottom,insets.getInsets(WindowInsetsCompat.Type.ime()).bottom));return insets;});
        ViewCompat.requestApplyInsets(root);ViewCompat.setAccessibilityHeading(findViewById(R.id.assignmentHeading),true);
        repository=new AssignmentRepository(this);((MaterialToolbar)findViewById(R.id.assignmentToolbar)).setNavigationOnClickListener(v->back());
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){@Override public void handleOnBackPressed(){back();}});
        Spinner filter=findViewById(R.id.assignmentFilter);filter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,getResources().getStringArray(R.array.assignment_filters)));
        filter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int n,long id){String next=new String[]{"","new","taken","in_progress"}[n];
                if(started&&!busy&&uncertain==null&&!next.equals(status)){status=next;orderHistory.clear();loadOrders("",0);}}
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){ }
        });
        findViewById(R.id.assignmentReload).setOnClickListener(v->{if(!busy){if(scope==0||!repository.current(scope))restart();else loadOrders(orderCursor,0);}});
        findViewById(R.id.assignmentNext).setOnClickListener(v->page(true));findViewById(R.id.assignmentPrevious).setOnClickListener(v->page(false));
        findViewById(R.id.assignmentConfirm).setOnClickListener(v->prepare());findViewById(R.id.assignmentRetry).setOnClickListener(v->{if(uncertain!=null&&!busy)confirm(uncertain);});
        findViewById(R.id.assignmentRows).setSaveFromParentEnabled(false);render();
    }
    @Override protected void onStart(){super.onStart();started=true;restart();}
    @Override protected void onStop(){started=false;generation++;repository.cancel();scope=0;busy=false;if(confirmation!=null){confirmation.dismiss();confirmation=null;}
        orders=null;options=null;selected=null;access=null;uncertain=null;stage=ORDERS;status="";orderCursor="";optionCursor="";orderHistory.clear();optionHistory.clear();message=0;
        ((Spinner)findViewById(R.id.assignmentFilter)).setSelection(0);clearReason();render();super.onStop();}
    @Override protected void onDestroy(){repository.cancel();executor.shutdownNow();super.onDestroy();}
    private void restart(){if(!started||busy)return;orders=null;options=null;selected=null;uncertain=null;access=null;orderHistory.clear();optionHistory.clear();clearReason();
        try{scope=repository.bind();loadOrders("",0);}catch(IOException failure){scope=0;message=error(failure);render();}}
    private interface ReadTask<T>{AssignmentRepository.Read<T> run(long expected)throws IOException;}
    private interface Accept<T>{void accept(T value);}
    private <T> void read(ReadTask<T> task,Accept<T> accept){
        if(!started||busy||scope==0)return;int ticket=++generation;long expected=scope;busy=true;message=R.string.assignment_loading;render();
        executor.execute(()->{try{AssignmentRepository.Read<T> result=task.run(expected);runOnUiThread(()->{if(!active(ticket))return;
                    if(!repository.current(expected)){failed(null,true);return;}busy=false;access=result.access;message=uncertain==null?0:R.string.assignment_uncertain_checked;
                    if(uncertain!=null&&!access.canAssign(uncertain.reassign)){uncertain=null;message=R.string.assignment_permission;if(confirmation!=null){confirmation.dismiss();confirmation=null;}}
                    accept.accept(result.data);render();});
            }catch(IOException|RuntimeException failure){runOnUiThread(()->{if(active(ticket))failed(failure,!repository.current(expected));});}});
    }
    private void loadOrders(String cursor,int direction){
        if(busy)return;stage=ORDERS;selected=null;options=null;clearReason();
        String prior=orderCursor;read(expected->repository.orders(expected,status,cursor),page->{
            if(direction>0)orderHistory.addLast(prior);else if(direction<0)orderHistory.removeLast();orderCursor=cursor;orders=page;});
    }
    private void loadOptions(Assignments.Order order,String cursor,int direction){
        if(busy||uncertain!=null)return;stage=OPTIONS;selected=null;clearReason();String prior=optionCursor;
        read(expected->repository.options(expected,order,cursor),page->{if(direction>0)optionHistory.addLast(prior);else if(direction<0)optionHistory.removeLast();optionCursor=cursor;options=page;});
    }
    private void page(boolean forward){if(busy||uncertain!=null)return;
        if(stage==ORDERS&&orders!=null){String next=forward?orders.nextCursor:orderHistory.peekLast();if(next!=null)loadOrders(next,forward?1:-1);}
        else if(stage==OPTIONS&&options!=null){String next=forward?options.nextCursor:optionHistory.peekLast();if(next!=null)loadOptions(options.order,next,forward?1:-1);}}
    private void prepare(){if(busy||uncertain!=null||options==null||selected==null)return;
        if(access==null||!access.canAssign(options.order.needsReassignPermission())){failed(null,true);return;}
        try{AssignmentAction action=AssignmentAction.create(access,scope,options,selected,((EditText)findViewById(R.id.assignmentReason)).getText().toString());confirm(action);}
        catch(IOException invalid){((EditText)findViewById(R.id.assignmentReason)).setError(getString(R.string.assignment_reason_required));}}
    private void confirm(AssignmentAction action){
        if(busy||!started)return;if(!repository.current(action.scope)){failed(null,true);return;}
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(findViewById(R.id.assignmentReason).getWindowToken(),0);
        confirmation=new AlertDialog.Builder(this).setTitle(action.reassign?R.string.assignment_reassign_title:R.string.assignment_confirm_title)
            .setMessage(orderText(action.order)+"\n\n"+optionText(action.option)+"\n\n"+getString(R.string.assignment_confirm_reason,action.reason)
                    +(action.reassign?"\n\n"+getString(R.string.assignment_replace_notice):""))
            .setNegativeButton(R.string.assignment_no,null).setPositiveButton(R.string.assignment_yes,(dialog,which)->{
                confirmation=null;if(!busy&&started){if(!repository.current(action.scope)){failed(null,true);return;}clearReason();send(action);}}).create();
        if(confirmation.getWindow()==null){confirmation=null;return;}
        confirmation.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);confirmation.getWindow().getDecorView().setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        confirmation.show();confirmation.getButton(AlertDialog.BUTTON_POSITIVE).setFilterTouchesWhenObscured(true);
    }
    private void send(AssignmentAction action){
        if(!started||busy)return;int ticket=++generation;orders=null;busy=true;message=R.string.assignment_saving;render();
        executor.execute(()->{boolean ack=false;try{repository.apply(action);ack=true;
                AssignmentRepository.Read<Assignments.Orders> result=repository.orders(action.scope,status,orderCursor);
                runOnUiThread(()->{if(!active(ticket))return;if(!repository.current(action.scope)){failed(null,true);return;}
                    busy=false;uncertain=null;access=result.access;orders=result.data;options=null;selected=null;stage=ORDERS;message=R.string.assignment_updated;render();});
            }catch(IOException|RuntimeException failure){final boolean acknowledged=ack;runOnUiThread(()->{if(!active(ticket))return;
                if(!repository.current(action.scope)){failed(null,true);return;}busy=false;options=null;selected=null;
                boolean definitive=failure instanceof AdminApiClient.Failure&&((AdminApiClient.Failure)failure).status>=400&&((AdminApiClient.Failure)failure).status<500&&((AdminApiClient.Failure)failure).status!=408;
                if(acknowledged||definitive){uncertain=null;orders=null;stage=ORDERS;message=acknowledged?R.string.assignment_ack_reload:error(failure);if(isDenied(failure))access=null;}
                else{uncertain=action;message=R.string.assignment_uncertain;}render();});}});
    }
    private boolean active(int ticket){return started&&ticket==generation&&!isFinishing();}
    private void failed(Throwable failure,boolean changed){busy=false;orders=null;options=null;selected=null;clearReason();stage=ORDERS;
        if(changed||isDenied(failure)){uncertain=null;access=null;if(changed){scope=0;repository.cancel();}}
        message=changed?R.string.assignment_session:error(failure);render();}
    private boolean isDenied(Throwable failure){return failure instanceof AdminApiClient.Failure&&(((AdminApiClient.Failure)failure).status==401||((AdminApiClient.Failure)failure).status==403);}
    private int error(Throwable failure){if(failure instanceof AdminApiClient.Failure){int status=((AdminApiClient.Failure)failure).status;
        if(status==401)return R.string.assignment_session;if(status==403)return R.string.assignment_permission;if(status==409)return R.string.assignment_conflict;
        if(status==404)return R.string.assignment_not_found;if(status==422)return R.string.assignment_blocked;}return R.string.assignment_failed;}
    private void back(){if(busy||uncertain!=null||stage==ORDERS){finish();return;}if(stage==FORM&&options!=null){loadOptions(options.order,optionCursor,0);return;}loadOrders(orderCursor,0);}
    private void clearReason(){((EditText)findViewById(R.id.assignmentReason)).setText("");}
    private void render(){
        show(R.id.assignmentProgress,busy);show(R.id.assignmentFilter,!busy&&stage==ORDERS&&uncertain==null);
        ((TextView)findViewById(R.id.assignmentMessage)).setText(message==0?"":getString(message));
        ((TextView)findViewById(R.id.assignmentHeading)).setText(uncertain!=null?R.string.assignment_uncertain_title:stage==ORDERS?R.string.assignment_choose_order:stage==OPTIONS?R.string.assignment_choose_pair:R.string.assignment_form_title);
        LinearLayout rows=findViewById(R.id.assignmentRows);rows.removeAllViews();rows.setVisibility(busy?View.GONE:View.VISIBLE);
        show(R.id.assignmentForm,!busy&&uncertain==null&&stage==FORM&&selected!=null);show(R.id.assignmentConfirm,!busy&&uncertain==null&&stage==FORM&&selected!=null);
        show(R.id.assignmentRetry,!busy&&uncertain!=null);findViewById(R.id.assignmentReload).setEnabled(!busy);
        show(R.id.assignmentPaging,!busy&&uncertain==null&&((stage==ORDERS&&orders!=null)||(stage==OPTIONS&&options!=null)));
        boolean previous=stage==ORDERS?!orderHistory.isEmpty():!optionHistory.isEmpty();
        String next=stage==ORDERS?(orders==null?null:orders.nextCursor):(options==null?null:options.nextCursor);
        findViewById(R.id.assignmentPrevious).setEnabled(previous);findViewById(R.id.assignmentNext).setEnabled(next!=null);
        ((TextView)findViewById(R.id.assignmentSummary)).setText(!busy&&stage==FORM&&options!=null&&selected!=null?orderText(options.order)+"\n\n"+optionText(selected):"");
        if(busy)return;
        if(uncertain!=null){label(rows,getString(R.string.assignment_original)+"\n"+orderText(uncertain.order)+"\n\n"+optionText(uncertain.option)+"\n\n"+getString(R.string.assignment_confirm_reason,uncertain.reason));
            if(orders!=null)for(Assignments.Order current:orders.rows)if(current.id.equals(uncertain.order.id))label(rows,getString(R.string.assignment_current_state,stateText(current)));return;}
        if(stage==ORDERS&&orders!=null){if(orders.rows.isEmpty())label(rows,getString(R.string.assignment_empty_orders));
            for(Assignments.Order order:orders.rows)row(rows,orderText(order),access!=null&&access.canAssign(order.needsReassignPermission()),()->{optionHistory.clear();optionCursor="";loadOptions(order,"",0);});}
        else if(stage==OPTIONS&&options!=null){label(rows,orderText(options.order));if(options.rows.isEmpty())label(rows,getString(R.string.assignment_empty_options));
            for(Assignments.Option option:options.rows){if(!option.eligible){label(rows,optionText(option));}else row(rows,optionText(option),access!=null&&access.canAssign(options.order.needsReassignPermission())&&(options.order.newAssignment()||options.order.reassignable()),()->{selected=option;stage=FORM;clearReason();message=0;((Button)findViewById(R.id.assignmentConfirm)).setText(options.order.reassignable()?R.string.assignment_reassign:R.string.assignment_prepare);render();});
                for(String blocker:option.blockers)label(rows,getString(blocker(blocker)));}}
    }
    private void row(LinearLayout rows,String text,boolean enabled,Runnable action){MaterialButton button=new MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(text);button.setAllCaps(false);button.setSingleLine(false);button.setMaxLines(Integer.MAX_VALUE);button.setEllipsize(null);button.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        button.setMinHeight(dp(48));button.setSaveEnabled(false);button.setFilterTouchesWhenObscured(true);button.setEnabled(enabled);button.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(8);rows.addView(button,params);}
    private void label(LinearLayout rows,String text){TextView view=new TextView(this);view.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);view.setText(text);view.setSaveEnabled(false);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(8);params.bottomMargin=dp(8);rows.addView(view,params);}
    private String orderText(Assignments.Order order){String tariff=order.tariff==null?getString(R.string.assignment_unknown):getString("business".equals(order.tariff)?R.string.assignment_business:"minivan".equals(order.tariff)?R.string.assignment_minivan:R.string.assignment_comfort);
        return getString(R.string.assignment_order_row,known(order.from),known(order.to),java.time.LocalDate.parse(order.date).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.uuuu",java.util.Locale.ROOT)),tariff,stateText(order),order.id);}
    private String stateText(Assignments.Order order){return getString("new".equals(order.status)?R.string.assignment_new:"taken".equals(order.status)?R.string.assignment_taken:R.string.assignment_in_progress);}
    private String optionText(Assignments.Option option){return getString(R.string.assignment_option_row,known(option.fullName),option.make,option.model,option.plate,getString("on_line".equals(option.state)?R.string.assignment_on_line:R.string.assignment_off_line));}
    private String known(String text){return text==null||text.isEmpty()?getString(R.string.assignment_unknown):text;}
    private int blocker(String code){switch(code){case "driver_not_active":case "driver_not_found":return R.string.assignment_driver_inactive;case "driver_not_approved":return R.string.assignment_driver_approval;
        case "identity_verification_required":return R.string.assignment_identity;case "vehicle_not_found":case "vehicle_not_approved":return R.string.assignment_vehicle;
        case "vehicle_photo_control_required":return R.string.assignment_photo;case "active_vehicle_mismatch":return R.string.assignment_vehicle_mismatch;
        case "order_not_assignable":case "order_not_reassignable":return R.string.assignment_order_blocked;case "assignment_unchanged":return R.string.assignment_unchanged;default:return R.string.assignment_blocked;}}
    private void show(int id,boolean visible){findViewById(id).setVisibility(visible?View.VISIBLE:View.GONE);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
