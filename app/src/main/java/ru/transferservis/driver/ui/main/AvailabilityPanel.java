package ru.transferservis.driver.ui.main;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.material.slider.Slider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ru.transferservis.driver.R;
import ru.transferservis.driver.data.registration.Registration;
import ru.transferservis.driver.data.registration.RegistrationRepository;

/** Bottom-of-main availability. A gesture requests a transition; only server state enables radar. */
public final class AvailabilityPanel implements DefaultLifecycleObserver {
    private final AppCompatActivity activity;
    private final View root;
    private final RegistrationRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Registration.Vehicle> approved = new ArrayList<>();
    private Registration snapshot;
    private boolean started, busy, tracking, requested;
    private int generation;
    private long scope;
    public AvailabilityPanel(AppCompatActivity activity, View root, RegistrationRepository repository) {
        this.activity = activity; this.root = root; this.repository = repository;
        activity.getLifecycle().addObserver(this);
        root.findViewById(R.id.availabilityReload).setOnClickListener(view -> reload());
        root.findViewById(R.id.availabilityGoOffline).setOnClickListener(view -> transition(false));
        Slider slider = root.findViewById(R.id.availabilitySlider);
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override public void onStartTrackingTouch(Slider view) { tracking = true; }
            @Override public void onStopTrackingTouch(Slider view) {
                tracking = false;
                if (view.getValue() >= 100) transition(true);
                if (!busy) view.setValue(0);
            }
        });
        // Keyboard/TalkBack SET_PROGRESS does not dispatch touch callbacks.
        slider.addOnChangeListener((view, value, fromUser) -> { if (fromUser && !tracking && value >= 100) transition(true); });
    }
    public void start() {
        requested = true;
        if (!activity.getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED) || activity.isFinishing()) return;
        stop(); requested = true; started = true; root.setVisibility(View.VISIBLE); reload();
    }
    public void stop() {
        requested = false; started = false; generation++; repository.cancel(); snapshot = null; approved.clear(); busy = false; tracking = false;
        ((Spinner) root.findViewById(R.id.availabilityVehicle)).setAdapter(null);
        ((Slider) root.findViewById(R.id.availabilitySlider)).setValue(0);
        message(0); hideControls(); root.setVisibility(View.GONE);
    }
    @Override public void onStart(LifecycleOwner owner) { if (requested) start(); }
    @Override public void onStop(LifecycleOwner owner) { stop(); }
    @Override public void onDestroy(LifecycleOwner owner) { stop(); executor.shutdownNow(); }
    private void reload() {
        if (!started || busy) return;
        snapshot = null; approved.clear(); hideControls(); setBusy(true); message(R.string.availability_loading);
        int ticket = ++generation;
        final long expected;
        try { scope = repository.bind(); expected = scope; } catch (IOException failure) { failed(failure, false); return; }
        executor.execute(() -> {
            try {
                Registration loaded = repository.load(expected);
                activity.runOnUiThread(() -> {
                    if (!active(ticket)) return;
                    if (!repository.current(expected)) { failed(new RegistrationRepository.Failure(401, ""), false); return; }
                    snapshot = loaded; setBusy(false); render();
                });
            } catch (IOException failure) { deliver(ticket, failure, false); }
        });
    }
    private void render() {
        hideControls(); approved.clear();
        if (snapshot == null || snapshot.availability == null || snapshot.availability.revision == null) { message(R.string.availability_missing); return; }
        root.findViewById(R.id.availabilityReload).setVisibility(View.GONE);
        if (snapshot.availability.online()) {
            message(0); root.findViewById(R.id.availabilityOnline).setVisibility(View.VISIBLE); return;
        }
        root.findViewById(R.id.availabilityOffline).setVisibility(View.VISIBLE);
        List<String> labels = new ArrayList<>(); labels.add(activity.getString(R.string.availability_choose_vehicle));
        for (Registration.Vehicle vehicle : snapshot.vehicles) if ("approved".equals(vehicle.status)) {
            approved.add(vehicle); labels.add(activity.getString(R.string.availability_vehicle_row, vehicle.make, vehicle.model, vehicle.plate));
        }
        Spinner picker = root.findViewById(R.id.availabilityVehicle);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); picker.setAdapter(adapter);
        int selected = 0;
        for (int i = 0; i < approved.size(); i++) if (approved.get(i).id.equals(snapshot.availability.activeVehicleId)) selected = i + 1;
        picker.setSelection(selected);
        int reason = !"approved".equals(snapshot.status) ? R.string.availability_approval
                : !"verified".equals(snapshot.identityStatus) ? R.string.availability_identity
                : approved.isEmpty() ? R.string.availability_no_vehicle : R.string.availability_offline;
        message(reason);
        boolean eligible = reason == R.string.availability_offline;
        picker.setEnabled(eligible);
        root.findViewById(R.id.availabilitySlider).setEnabled(eligible && selected > 0);
        picker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                root.findViewById(R.id.availabilitySlider).setEnabled(!busy && eligible && position > 0);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { root.findViewById(R.id.availabilitySlider).setEnabled(false); }
        });
    }
    private void transition(boolean online) {
        if (!started || busy || snapshot == null || snapshot.availability == null || snapshot.availability.revision == null) return;
        if (!repository.current(scope)) { failed(new RegistrationRepository.Failure(401, ""), false); return; }
        if (online == snapshot.availability.online()) return;
        String vehicle = snapshot.availability.activeVehicleId;
        if (online) {
            int choice = ((Spinner) root.findViewById(R.id.availabilityVehicle)).getSelectedItemPosition();
            if (!"approved".equals(snapshot.status) || !"verified".equals(snapshot.identityStatus) || choice < 1 || choice > approved.size()) return;
            vehicle = approved.get(choice - 1).id;
        }
        // Keep the active vehicle when going offline: never silently switch/null it during an active order.
        final String chosen = vehicle.isEmpty() ? null : vehicle;
        final long revision = snapshot.availability.revision, expected = scope;
        final String key = "availability-" + UUID.randomUUID();
        int ticket = ++generation;
        hideControls(); setBusy(true); message(R.string.availability_saving);
        executor.execute(() -> {
            try {
                repository.availability(expected, chosen, online ? "on_line" : "off_line", revision, key);
                Registration loaded = repository.load(expected);
                activity.runOnUiThread(() -> {
                    if (!active(ticket)) return;
                    if (!repository.current(expected)) { failed(new RegistrationRepository.Failure(401, ""), true); return; }
                    snapshot = loaded; setBusy(false); render();
                });
            } catch (IOException failure) { deliver(ticket, failure, true); }
        });
    }
    private void deliver(int ticket, IOException failure, boolean mutation) { activity.runOnUiThread(() -> { if (active(ticket)) failed(failure, mutation); }); }
    private boolean active(int ticket) { return started && generation == ticket && !activity.isFinishing(); }
    private void failed(IOException failure, boolean mutation) {
        snapshot = null; approved.clear(); hideControls(); setBusy(false);
        ((Spinner) root.findViewById(R.id.availabilityVehicle)).setAdapter(null);
        RegistrationRepository.Failure api = failure instanceof RegistrationRepository.Failure ? (RegistrationRepository.Failure) failure : null;
        int text = mutation ? R.string.availability_uncertain : R.string.availability_network;
        if (api != null) {
            if (api.status == 401) text = R.string.availability_session;
            else if ("driver_not_approved".equals(api.code)) text = R.string.availability_approval;
            else if ("identity_verification_required".equals(api.code)) text = R.string.availability_identity;
            else if ("vehicle_photo_control_required".equals(api.code)) text = R.string.availability_photo;
            else if ("active_vehicle_switch_requires_offline".equals(api.code)) text = R.string.availability_switch;
            else if ("availability_preconditions_failed".equals(api.code)) text = R.string.availability_preconditions;
            else if ("vehicle_not_approved".equals(api.code) || "vehicle_not_found".equals(api.code)) text = R.string.availability_vehicle_blocked;
            else if (api.status == 409) text = R.string.availability_conflict;
            else if (api.status == 403) text = R.string.availability_forbidden;
        }
        message(text);
    }
    private void hideControls() {
        root.findViewById(R.id.availabilityReload).setVisibility(View.VISIBLE);
        root.findViewById(R.id.availabilityOffline).setVisibility(View.GONE);
        root.findViewById(R.id.availabilityOnline).setVisibility(View.GONE);
        ((Slider) root.findViewById(R.id.availabilitySlider)).setValue(0);
    }
    private void setBusy(boolean value) {
        busy = value; root.findViewById(R.id.availabilityProgress).setVisibility(value ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.availabilityReload).setEnabled(!value);
    }
    private void message(int text) { ((TextView) root.findViewById(R.id.availabilityMessage)).setText(text == 0 ? "" : activity.getString(text)); }
}
