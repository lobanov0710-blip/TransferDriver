package ru.transferservis.admin.ui;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
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
        State(AdminAccess access, List<RegistrationRow> rows, boolean loading, boolean loaded, int error) {
            this(access,rows,loading,loaded,error,null,null,Collections.emptyList());
        }
        State(AdminAccess access, List<RegistrationRow> rows, boolean loading, boolean loaded, int error,
                OwnerMfa.Challenge challenge, OwnerMfa.Enrollment enrollment, List<String> recovery) {
            this.access=access;registrations=rows;this.loading=loading;queueLoaded=loaded;this.error=error;
            this.challenge=challenge;this.enrollment=enrollment;recoveryCodes=recovery;
        }
        public boolean hasTransientSecurityData() { return challenge!=null||enrollment!=null||!recoveryCodes.isEmpty(); }
        State pending(boolean busy,int failure) {
            return new State(null,Collections.emptyList(),busy,false,failure,challenge,enrollment,recoveryCodes);
        }
    }
    private final AdminRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(idle(0));
    private final Handler main = new Handler(Looper.getMainLooper());
    private long request;
    private boolean initialized;
    private interface Task { State run() throws IOException; }
    public AdminViewModel(Application app) { super(app); repository=((AdminApplication)app).repository(); }
    private static State idle(int error) { return new State(null,Collections.emptyList(),false,false,error); }
    public LiveData<State> state() { return state; }
    public void initialize() { if(!initialized){initialized=true;if(repository.hasSession())restore();} }
    public void restore() { submit(()->new State(repository.me(),Collections.emptyList(),false,false,0)); }
    public void login(String login,String password) {
        if(login.trim().isEmpty()||password.isEmpty()){state.setValue(idle(R.string.login_required));return;}
        long generation=repository.generation();
        submit(()->new State(repository.login(login.trim(),password,generation),Collections.emptyList(),false,false,0));
    }
    public void verify(String code,boolean recovery) {
        State before=state.getValue();long generation=repository.generation();
        if(before==null||before.challenge==null||!before.challenge.active()){state.setValue(idle(R.string.mfa_expired));return;}
        submit(()->new State(repository.verifyMfa(before.challenge,code,recovery,generation),Collections.emptyList(),false,false,0));
    }
    public void startEnrollment(String bootstrap) {
        State before=state.getValue();
        if(before==null||before.challenge==null||!before.challenge.active()){state.setValue(idle(R.string.mfa_expired));return;}
        submit(()->new State(null,Collections.emptyList(),false,false,0,before.challenge,
                repository.startEnrollment(before.challenge,bootstrap),Collections.emptyList()));
    }
    public void confirmEnrollment(String code) {
        State before=state.getValue();
        if(before==null||before.enrollment==null||!before.enrollment.active()){state.setValue(idle(R.string.mfa_expired));return;}
        submit(()->new State(null,Collections.emptyList(),false,false,0,null,null,
                repository.confirmEnrollment(before.enrollment,code)));
    }
    public void acknowledgeRecoveryCodes() { request++;state.setValue(idle(0)); }
    public void queue() {
        submit(()->{AdminRepository.QueueResult result=repository.registrations();
            return new State(result.access,RegistrationRow.parse(result.response),false,true,0);});
    }
    private void submit(Task task) {
        State before=state.getValue();
        if(before!=null&&before.loading)return; // Do not replay one-time MFA submissions on double tap.
        long id=++request,generation=repository.generation();
        state.setValue(before==null?idle(0).pending(true,0):before.pending(true,0));
        repository.executor.execute(()->{
            State next;
            try { next=task.run(); }
            catch(OwnerMfa.Required required){next=new State(null,Collections.emptyList(),false,false,0,required.challenge,null,Collections.emptyList());}
            catch(IOException|RuntimeException failure){
                boolean active=before!=null&&before.challenge!=null&&before.challenge.active()
                        &&(before.enrollment==null||before.enrollment.active());
                next=active?before.pending(false,R.string.mfa_failed):idle(R.string.request_failed);
            }
            State result=next;
            main.post(()->{if(id==request&&generation==repository.generation())state.setValue(result);});
        });
    }
    public void logout() { request++;repository.logout();state.setValue(idle(0)); }
    public void hideSecurityFlow() {
        State current=state.getValue();if(current!=null&&current.hasTransientSecurityData())logout();
    }
    @Override protected void onCleared(){request++;super.onCleared();}
}
