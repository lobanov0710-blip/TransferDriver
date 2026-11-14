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
import ru.transferservis.admin.data.RegistrationRow;

public final class AdminViewModel extends AndroidViewModel {
    public static final class State {
        public final AdminAccess access;
        public final List<RegistrationRow> registrations;
        public final boolean loading, queueLoaded;
        public final int error;
        State(AdminAccess access, List<RegistrationRow> rows, boolean loading, boolean queueLoaded, int error) {
            this.access = access; registrations = rows; this.loading = loading; this.queueLoaded = queueLoaded; this.error = error;
        }
    }
    private final AdminRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(null, Collections.emptyList(), false, false, 0));
    private final Handler main = new Handler(Looper.getMainLooper());
    private long request;
    private boolean initialized;
    private interface Task { State run() throws IOException; }
    public AdminViewModel(Application app) { super(app); repository = ((AdminApplication) app).repository(); }
    public LiveData<State> state() { return state; }
    public void initialize() { if (!initialized) { initialized = true; if (repository.hasSession()) restore(); } }
    public void restore() {
        submit(() -> new State(repository.me(), Collections.emptyList(), false, false, 0));
    }
    public void login(String login, String password) {
        if (login.trim().isEmpty() || password.isEmpty()) {
            state.setValue(new State(null, Collections.emptyList(), false, false, R.string.login_required)); return;
        }
        long generation = repository.generation();
        submit(() -> new State(repository.login(login.trim(), password, generation), Collections.emptyList(), false, false, 0));
    }
    public void queue() {
        submit(() -> {
            AdminRepository.QueueResult result = repository.registrations();
            return new State(result.access, RegistrationRow.parse(result.response), false, true, 0);
        });
    }
    private void submit(Task task) {
        long id = ++request, generation = repository.generation();
        state.setValue(new State(null, Collections.emptyList(), true, false, 0));
        repository.executor.execute(() -> {
            State next;
            try { next = task.run(); }
            catch (IOException | RuntimeException failure) {
                next = new State(null, Collections.emptyList(), false, false, R.string.request_failed);
            }
            State result = next;
            main.post(() -> { if (id == request && generation == repository.generation()) state.setValue(result); });
        });
    }
    public void logout() {
        request++; repository.logout();
        state.setValue(new State(null, Collections.emptyList(), false, false, 0));
    }
    @Override protected void onCleared() { request++; super.onCleared(); }
}
