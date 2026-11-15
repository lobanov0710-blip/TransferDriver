package ru.transferservis.admin.ui;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import ru.transferservis.admin.AdminApplication;
import ru.transferservis.admin.R;
import ru.transferservis.admin.data.AdminAccess;
import ru.transferservis.admin.data.AdminRepository;
import ru.transferservis.admin.data.OwnerMfa;
import ru.transferservis.admin.data.RegistrationRow;
import ru.transferservis.admin.data.RegistrationReview;
import ru.transferservis.admin.data.ReviewAction;
import ru.transferservis.admin.data.AdminApiClient;

public final class AdminViewModel extends AndroidViewModel {
    public static final class State {
        public final AdminAccess access;
        public final List<RegistrationRow> registrations;
        public final boolean loading, queueLoaded;
        public final int error;
        // Only volatile memory for a currently visible security flow; never SavedStateHandle/Bundle.
        public final OwnerMfa.Challenge challenge;
        public final OwnerMfa.Enrollment enrollment;
        public final List<String> recoveryCodes;
        public final ReviewScreen reviewScreen;
        State(AdminAccess access, List<RegistrationRow> rows, boolean loading, boolean loaded, int error) {
            this(access,rows,loading,loaded,error,null,null,Collections.emptyList());
        }
        State(AdminAccess access, List<RegistrationRow> rows, boolean loading, boolean loaded, int error,
                OwnerMfa.Challenge challenge, OwnerMfa.Enrollment enrollment, List<String> recovery) {
            this(access,rows,loading,loaded,error,challenge,enrollment,recovery,null);
        }
        State(AdminAccess access,List<RegistrationRow> rows,boolean loading,boolean loaded,int error,
                OwnerMfa.Challenge challenge,OwnerMfa.Enrollment enrollment,List<String> recovery,ReviewScreen screen) {
            this.access=access;registrations=rows;this.loading=loading;queueLoaded=loaded;this.error=error;
            this.challenge=challenge;this.enrollment=enrollment;recoveryCodes=recovery;
            reviewScreen=screen;
        }
        public boolean hasTransientSecurityData() { return challenge!=null||enrollment!=null||!recoveryCodes.isEmpty(); }
        State pending(boolean busy,int failure) {
            return new State(null,Collections.emptyList(),busy,false,failure,challenge,enrollment,recoveryCodes,
                    reviewScreen==null?null:new ReviewScreen(reviewScreen.registration,null,"",reviewScreen.uncertain));
        }
    }
    private final AdminRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(idle(0));
    private final Handler main = new Handler(Looper.getMainLooper());
    private long request;
    private boolean initialized;
    private boolean authenticationInFlight;
    private String securityFlowId;
    private long securityDeadlineElapsed;
    private final Runnable expireSecurity=()->expireSecurityFlow();
    private interface Task { State run() throws IOException; }
    public AdminViewModel(Application app) { super(app); repository=((AdminApplication)app).repository(); }
    private static State idle(int error) { return new State(null,Collections.emptyList(),false,false,error); }
    public LiveData<State> state() { return state; }
    public void initialize() { if(!initialized){initialized=true;if(repository.hasSession())restore();} }
    public void restore() { submit(()->new State(repository.me(),Collections.emptyList(),false,false,0)); }
    public void login(String login,String password) {
        if(login.trim().isEmpty()||password.isEmpty()){publish(idle(R.string.login_required));return;}
        long generation=repository.generation();
        submit(()->new State(repository.login(login.trim(),password,generation),Collections.emptyList(),false,false,0),true);
    }
    public void verify(String code,boolean recovery) {
        expireSecurityFlow();
        State before=state.getValue();long generation=repository.generation();
        if(before==null||before.challenge==null||!before.challenge.active()){publish(idle(R.string.mfa_expired));return;}
        submit(()->new State(repository.verifyMfa(before.challenge,code,recovery,generation),Collections.emptyList(),false,false,0),true);
    }
    public void startEnrollment(String bootstrap) {
        expireSecurityFlow();
        State before=state.getValue();
        if(before==null||before.challenge==null||!before.challenge.active()){publish(idle(R.string.mfa_expired));return;}
        submit(()->new State(null,Collections.emptyList(),false,false,0,before.challenge,
                repository.startEnrollment(before.challenge,bootstrap),Collections.emptyList()),true);
    }
    public void confirmEnrollment(String code) {
        expireSecurityFlow();
        State before=state.getValue();
        if(before==null||before.enrollment==null||!before.enrollment.active()){publish(idle(R.string.mfa_expired));return;}
        submit(()->new State(null,Collections.emptyList(),false,false,0,null,null,
                repository.confirmEnrollment(before.enrollment,code)),true);
    }
    public void acknowledgeRecoveryCodes() { request++;publish(idle(0)); }
    public void queue() {
        queue("");
    }
    public void queue(String status) {
        submit(()->{AdminRepository.QueueResult result=repository.registrations(status);
            return new State(result.access,RegistrationRow.parse(result.response),false,true,0);});
    }
    private static State reviewing(AdminAccess access,ReviewScreen screen,int error) {
        return new State(access,Collections.emptyList(),false,false,error,null,null,Collections.emptyList(),screen);
    }
    public void openReview(String driverId) {
        State before=state.getValue();if(before==null||before.access==null)return;
        String actor=before.access.accountId;
        submit(()->{AdminRepository.ReviewResult r=repository.review(driverId,actor);
            return reviewing(r.access,new ReviewScreen(r.registration,null,"",null),0);});
    }
    public void closeReview(){request++;State current=state.getValue();publish(new State(current==null?null:current.access,Collections.emptyList(),false,false,0));}
    public void startReview(){State s=state.getValue();if(s==null||s.access==null||s.reviewScreen==null||s.loading)return;
        try{sendReview(ReviewAction.start(s.access,s.reviewScreen.registration));}
        catch(IOException invalid){publish(reviewing(s.access,s.reviewScreen,R.string.review_unavailable));}}
    public void decideReview(String itemId,String decision,String note){
        State s=state.getValue();if(s==null||s.access==null||s.reviewScreen==null||s.loading||s.reviewScreen.uncertain!=null)return;
        RegistrationReview.Item item=itemId==null?null:s.reviewScreen.registration.item(itemId);
        if(itemId!=null&&item==null)return;
        if(item!=null&&!item.isVehicle()&&"verified".equals(decision)
                &&(!item.id.equals(s.reviewScreen.previewId)||s.reviewScreen.preview==null)){
            publish(reviewing(s.access,s.reviewScreen,R.string.preview_first));return;
        }
        try{sendReview(ReviewAction.decide(s.access,s.reviewScreen.registration,item,decision,note));}
        catch(IOException invalid){publish(reviewing(s.access,s.reviewScreen,R.string.reason_required));}
    }
    public void retryReview(){State s=state.getValue();if(s!=null&&s.reviewScreen!=null&&s.reviewScreen.uncertain!=null)sendReview(s.reviewScreen.uncertain);}
    public void preview(String id){
        State s=state.getValue();if(s==null||s.access==null||s.reviewScreen==null||s.reviewScreen.uncertain!=null)return;
        RegistrationReview registration=s.reviewScreen.registration;RegistrationReview.Item item=registration.item(id);
        if(item==null)return;
        submit(()->reviewing(s.access,new ReviewScreen(registration,ReviewScreen.decode(repository.preview(registration,item,s.access.accountId)),id,null),0));
    }
    private void sendReview(ReviewAction action){
        State before=state.getValue();if(before==null||before.loading||before.reviewScreen==null)return;
        long id=++request,generation=repository.generation();
        publish(reviewing(before.access,new ReviewScreen(before.reviewScreen.registration,null,"",action),0).pending(true,0));
        repository.executor.execute(()->{
            State next;boolean acknowledged=false;AdminAccess access=before.access;
            try{
                if(generation!=repository.generation())throw new IOException("Session replaced");
                access=repository.applyReview(action);acknowledged=true;
                AdminRepository.ReviewResult r=repository.review(action.driverId,action.accountId);
                next=reviewing(r.access,new ReviewScreen(r.registration,null,"",null),0);
            }catch(IOException|RuntimeException failure){
                boolean definitive=failure instanceof AdminApiClient.Failure&&((AdminApiClient.Failure)failure).status>=400
                        &&((AdminApiClient.Failure)failure).status<500&&((AdminApiClient.Failure)failure).status!=408;
                if(acknowledged||definitive)next=new State(access,Collections.emptyList(),false,false,
                        acknowledged?R.string.review_reload:reviewError(failure));
                else next=reviewing(access,new ReviewScreen(before.reviewScreen.registration,null,"",action),R.string.review_uncertain);
            }
            State result=next;main.post(()->{if(id==request&&generation==repository.generation())
                publish(repository.currentAccount(action.accountId)?result:idle(R.string.request_failed));});
        });
    }
    private static int reviewError(Exception failure){
        if(failure instanceof AdminApiClient.Failure){AdminApiClient.Failure f=(AdminApiClient.Failure)failure;
            if(f.status==403)return R.string.review_permission_denied;
            if(f.status==409)return R.string.review_conflict;
            if(f.status==422)return R.string.review_incomplete;
        }return R.string.request_failed;
    }
    private void submit(Task task) { submit(task,false); }
    private void submit(Task task,boolean authentication) {
        State before=state.getValue();
        if(before!=null&&before.loading)return; // Do not replay one-time MFA submissions on double tap.
        authenticationInFlight=authentication;
        long id=++request,generation=repository.generation();
        publish(before==null?idle(0).pending(true,0):before.pending(true,0));
        repository.executor.execute(()->{
            State next;
            try { if(generation!=repository.generation())throw new IOException("Session replaced");next=task.run(); }
            catch(OwnerMfa.Required required){next=new State(null,Collections.emptyList(),false,false,0,required.challenge,null,Collections.emptyList());}
            catch(IOException|RuntimeException failure){
                boolean active=before!=null&&before.challenge!=null&&before.challenge.active()
                        &&(before.enrollment==null||before.enrollment.active());
                next=active?before.pending(false,R.string.mfa_failed):new State(before==null?null:before.access,Collections.emptyList(),false,false,reviewError(failure));
            }
            State result=next;
            main.post(()->{if(id==request&&generation==repository.generation()) {
                authenticationInFlight=false;
                publish(result.access==null||repository.currentAccount(result.access.accountId)?result:idle(R.string.request_failed));}});
        });
    }
    private void publish(State next) {
        main.removeCallbacks(expireSecurity);
        String id=next.challenge!=null?next.challenge.id:next.enrollment!=null?next.enrollment.id:null;
        if(id==null){securityFlowId=null;securityDeadlineElapsed=0;}
        else{
            long expiry=next.challenge!=null?next.challenge.expiresAt:next.enrollment.expiresAt;
            if(next.enrollment!=null)expiry=Math.min(expiry,next.enrollment.expiresAt);
            long now=SystemClock.elapsedRealtime(),remaining=Math.max(0,Math.min(300_000,expiry-System.currentTimeMillis()));
            securityDeadlineElapsed=id.equals(securityFlowId)?Math.min(securityDeadlineElapsed,now+remaining):now+remaining;
            securityFlowId=id;main.postDelayed(expireSecurity,Math.max(0,securityDeadlineElapsed-now));
        }
        state.setValue(next);
    }
    public void expireSecurityFlow(){
        State current=state.getValue();
        if(current!=null&&((current.challenge!=null&&!current.challenge.active())||(current.enrollment!=null&&!current.enrollment.active())
                ||(securityFlowId!=null&&SystemClock.elapsedRealtime()>=securityDeadlineElapsed))){
            request++;authenticationInFlight=false;repository.logout();publish(idle(R.string.mfa_expired));
        }
    }
    public void logout() { request++;authenticationInFlight=false;repository.logout();publish(idle(0)); }
    public void hideSecurityFlow() {
        State current=state.getValue();if(current==null)return;
        if(authenticationInFlight){request++;authenticationInFlight=false;repository.logout();publish(idle(R.string.mfa_background_cancelled));return;}
        if(!current.recoveryCodes.isEmpty()){logout();return;}
        if(current.challenge!=null||current.enrollment!=null){
            expireSecurityFlow();current=state.getValue();
            if(current.challenge==null&&current.enrollment==null)return;
            request++;publish(new State(null,Collections.emptyList(),false,false,0,current.challenge,
                    current.enrollment==null?null:current.enrollment.withoutSecret(),Collections.emptyList()));return;
        }
        if(current.reviewScreen!=null||current.queueLoaded||current.loading){request++;repository.cancelPending();publish(new State(current.access,Collections.emptyList(),false,false,0));}
    }
    @Override protected void onCleared(){request++;main.removeCallbacks(expireSecurity);if(authenticationInFlight)repository.logout();super.onCleared();}
}
