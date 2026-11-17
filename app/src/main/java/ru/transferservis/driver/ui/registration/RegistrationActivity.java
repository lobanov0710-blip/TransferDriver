package ru.transferservis.driver.ui.registration;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ru.transferservis.driver.R;
import ru.transferservis.driver.data.registration.Registration;
import ru.transferservis.driver.data.registration.RegistrationRepository;
import ru.transferservis.driver.security.SecureActivity;

/** Draft forms only. Documents, identity verification and admission are separate server gates. */
public final class RegistrationActivity extends SecureActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private RegistrationRepository repository;
    private Registration registration;
    private Registration.Vehicle editing;
    private boolean started, busy, vehicleScreen;
    private int generation;
    private long scope;
    private static final int[] INPUTS = {R.id.fullNameInput, R.id.birthDateInput, R.id.regionInput,
            R.id.vinInput, R.id.plateInput, R.id.makeInput, R.id.modelInput, R.id.modelYearInput, R.id.colorInput, R.id.capacityInput};

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_registration);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        boolean lightSurface = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK) != android.content.res.Configuration.UI_MODE_NIGHT_YES;
        androidx.core.view.WindowInsetsControllerCompat barsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        barsController.setAppearanceLightStatusBars(lightSurface);
        barsController.setAppearanceLightNavigationBars(lightSurface);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registrationRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int bottom = Math.max(bars.bottom, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
            view.setPadding(bars.left, bars.top, bars.right, bottom);
            return insets;
        });
        MaterialToolbar toolbar = findViewById(R.id.registrationToolbar);
        toolbar.setTitle(R.string.registration_title);
        toolbar.setNavigationIcon(R.drawable.ic_registration_back);
        toolbar.setNavigationContentDescription(R.string.registration_back);
        toolbar.setNavigationOnClickListener(view -> back());
        for (int id : new int[]{R.id.registrationProfileHeading, R.id.registrationVehiclesHeading, R.id.registrationVehicleHeading})
            ViewCompat.setAccessibilityHeading(findViewById(id), true);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { back(); }
        });
        repository = new RegistrationRepository(this);
        findViewById(R.id.registrationReload).setOnClickListener(view -> reload());
        findViewById(R.id.registrationSaveProfile).setOnClickListener(view -> saveProfile());
        findViewById(R.id.registrationAddVehicle).setOnClickListener(view -> openVehicle(null));
        findViewById(R.id.registrationSaveVehicle).setOnClickListener(view -> saveVehicle());
        findViewById(R.id.registrationCancel).setOnClickListener(view -> showOverview());
    }
    @Override protected void onStart() { super.onStart(); started = true; reload(); }
    @Override protected void onStop() {
        started = false; generation++; repository.cancel(); clear(); super.onStop();
    }
    @Override protected void onDestroy() { repository.cancel(); executor.shutdownNow(); super.onDestroy(); }
    private void back() { if (vehicleScreen && !busy) showOverview(); else finish(); }
    private void clear() {
        registration = null; editing = null; vehicleScreen = false; busy = false;
        ((TextView) findViewById(R.id.registrationSystemClass)).setText(R.string.registration_class_pending);
        for (int id : INPUTS) { input(id).setText(""); layout(id).setError(null); }
        ((TextView) findViewById(R.id.registrationPhone)).setText("");
        ((TextView) findViewById(R.id.registrationMessage)).setText("");
        ((LinearLayout) findViewById(R.id.registrationVehicles)).removeAllViews();
        findViewById(R.id.registrationOverview).setVisibility(View.GONE);
        findViewById(R.id.registrationVehicleEditor).setVisibility(View.GONE);
        for (int id : new int[]{R.id.baggageInput, R.id.usageInput}) ((Spinner) findViewById(id)).setSelection(0);
    }
    private void reload() {
        if (!started || busy) return;
        clear(); setBusy(true); message(getString(R.string.registration_loading));
        int ticket = ++generation;
        final long expectedScope;
        try { scope = repository.bind(); expectedScope = scope; } catch (IOException failure) { failed(failure, false); return; }
        executor.execute(() -> {
            try {
                Registration loaded = repository.load(expectedScope);
                runOnUiThread(() -> {
                    if (!active(ticket)) return;
                    if (!repository.current(scope)) { failed(new RegistrationRepository.Failure(401, ""), false); return; }
                    registration = loaded; setBusy(false); showOverview();
                });
            } catch (IOException failure) { deliverFailure(ticket, failure, false); }
        });
    }
    private boolean active(int ticket) { return started && generation == ticket && !isFinishing(); }
    private void setBusy(boolean value) {
        busy = value;
        findViewById(R.id.registrationProgress).setVisibility(value ? View.VISIBLE : View.GONE);
        findViewById(R.id.registrationReload).setEnabled(!value);
        for (int id : new int[]{R.id.registrationSaveProfile, R.id.registrationAddVehicle, R.id.registrationSaveVehicle, R.id.registrationCancel})
            findViewById(id).setEnabled(!value);
        for (int id : INPUTS) input(id).setEnabled(!value);
        for (int id : new int[]{R.id.baggageInput, R.id.usageInput}) findViewById(id).setEnabled(!value);
    }
    private void showOverview() {
        if (registration == null || busy) return;
        vehicleScreen = false; editing = null;
        for (int id : new int[]{R.id.vinInput, R.id.plateInput, R.id.makeInput, R.id.modelInput, R.id.modelYearInput, R.id.colorInput, R.id.capacityInput}) input(id).setText("");
        findViewById(R.id.registrationOverview).setVisibility(View.VISIBLE);
        findViewById(R.id.registrationVehicleEditor).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.registrationPhone)).setText(registration.phone);
        input(R.id.fullNameInput).setText(registration.fullName);
        input(R.id.birthDateInput).setText(Registration.displayDate(registration.birthDate));
        input(R.id.regionInput).setText(registration.region);
        for (int id : new int[]{R.id.fullNameInput, R.id.birthDateInput, R.id.regionInput}) input(id).setEnabled(registration.editable());
        findViewById(R.id.registrationSaveProfile).setEnabled(registration.editable());
        findViewById(R.id.registrationAddVehicle).setEnabled(!"rejected".equals(registration.status));
        message("Статус анкеты: " + status(registration.status));
        LinearLayout list = findViewById(R.id.registrationVehicles); list.removeAllViews();
        if (registration.vehicles.isEmpty()) {
            TextView empty = new TextView(this); empty.setText(R.string.registration_empty);
            empty.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
            empty.setPadding(0, dp(16), 0, dp(16)); list.addView(empty);
        }
        for (Registration.Vehicle vehicle : registration.vehicles) {
            Button row = new com.google.android.material.button.MaterialButton(this);
            row.setText(getString(R.string.registration_vehicle_row, vehicle.make, vehicle.model, vehicle.plate, status(vehicle.status)));
            row.setAllCaps(false); row.setSingleLine(false); row.setMinHeight(dp(64)); row.setSaveEnabled(false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.topMargin = dp(8);
            list.addView(row, params); row.setOnClickListener(view -> { if (!busy) openVehicle(vehicle); });
        }
        top();
    }
    private void openVehicle(Registration.Vehicle vehicle) {
        if (registration == null || busy) return;
        if (!repository.current(scope)) { failed(new RegistrationRepository.Failure(401, ""), false); return; }
        editing = vehicle; vehicleScreen = true;
        findViewById(R.id.registrationOverview).setVisibility(View.GONE);
        findViewById(R.id.registrationVehicleEditor).setVisibility(View.VISIBLE);
        int[] ids = {R.id.vinInput, R.id.plateInput, R.id.makeInput, R.id.modelInput, R.id.modelYearInput, R.id.colorInput, R.id.capacityInput};
        String[] values = vehicle == null ? new String[]{"", "", "", "", "", "", ""}
                : new String[]{vehicle.vin, vehicle.plate, vehicle.make, vehicle.model, vehicle.modelYear == null ? "" : vehicle.modelYear.toString(), vehicle.color, Integer.toString(vehicle.capacity)};
        boolean editable = vehicle == null || vehicle.editable();
        for (int i = 0; i < ids.length; i++) { input(ids[i]).setText(values[i]); input(ids[i]).setEnabled(editable); layout(ids[i]).setError(null); }
        String serverClass = vehicle == null ? null : vehicle.vehicleClass;
        int classLabel = "comfort".equals(serverClass) ? R.string.registration_class_comfort
                : "business".equals(serverClass) ? R.string.registration_class_business
                : "minivan".equals(serverClass) ? R.string.registration_class_minivan : R.string.registration_class_pending;
        ((TextView) findViewById(R.id.registrationSystemClass)).setText(classLabel);
        select(R.id.baggageInput, Registration.BAGGAGE, vehicle == null ? null : vehicle.baggage, editable);
        select(R.id.usageInput, Registration.USAGE, vehicle == null ? null : vehicle.usage, editable);
        findViewById(R.id.registrationSaveVehicle).setEnabled(editable);
        message(vehicle == null ? "Новый автомобиль" : "Статус автомобиля: " + status(vehicle.status)); top();
    }
    private void saveProfile() {
        if (registration == null || busy || !registration.editable()) return;
        boolean valid = check(R.id.fullNameInput, 2, 100, "Введите ФИО: от 2 до 100 символов");
        valid = check(R.id.regionInput, 1, 120, "Введите регион работы") && valid;
        String date = Registration.inputDate(text(R.id.birthDateInput));
        layout(R.id.birthDateInput).setError(date == null ? "Введите существующую дату: ДД.ММ.ГГГГ, не позднее сегодня" : null);
        if (!valid || date == null) { message("Проверьте отмеченные поля"); return; }
        JsonObject body = new JsonObject(); body.addProperty("fullName", text(R.id.fullNameInput));
        body.addProperty("birthDate", date); body.addProperty("region", text(R.id.regionInput)); body.addProperty("expectedRevision", registration.revision);
        long expectedScope = scope;
        mutate(expectedScope, () -> { repository.saveProfile(expectedScope, body); return false; }, false);
    }
    private void saveVehicle() {
        if (registration == null || busy || (editing != null && !editing.editable())) return;
        boolean valid = Registration.validVin(text(R.id.vinInput));
        layout(R.id.vinInput).setError(valid ? null : "VIN: 17 латинских букв и цифр, без I, O и Q");
        valid = check(R.id.plateInput, 1, 32, "Введите госномер") && valid;
        valid = check(R.id.makeInput, 1, 80, "Введите марку") && valid;
        valid = check(R.id.modelInput, 1, 80, "Введите модель") && valid;
        valid = check(R.id.colorInput, 1, 80, "Укажите цвет кузова: от 1 до 80 символов") && valid;
        int seats = 0; try { seats = Integer.parseInt(text(R.id.capacityInput)); } catch (NumberFormatException ignored) { }
        boolean capacityValid = seats >= 1 && seats <= 20;
        layout(R.id.capacityInput).setError(capacityValid ? null : "От 1 до 20 пассажирских мест");
        Integer modelYear = Registration.inputModelYear(text(R.id.modelYearInput));
        layout(R.id.modelYearInput).setError(modelYear == null ? "Введите год выпуска" : null);
        String baggage = selected(R.id.baggageInput, Registration.BAGGAGE);
        String usage = selected(R.id.usageInput, Registration.USAGE);
        if (!valid || !capacityValid || modelYear == null || baggage == null || usage == null) {
            message("Проверьте поля и выберите объём багажа и основание использования"); return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("vin", text(R.id.vinInput).toUpperCase(Locale.ROOT)); body.addProperty("plate", text(R.id.plateInput));
        body.addProperty("make", text(R.id.makeInput)); body.addProperty("model", text(R.id.modelInput));
        body.addProperty("color", text(R.id.colorInput)); body.addProperty("capacity", seats);
        body.addProperty("modelYear", modelYear); body.addProperty("baggageCapacity", baggage); body.addProperty("usageBasis", usage);
        String id = editing == null ? null : editing.id;
        if (editing != null) body.addProperty("expectedRevision", editing.revision);
        long expectedScope = scope;
        boolean dataChanged = editing != null && (!editing.make.equals(text(R.id.makeInput))
                || !editing.model.equals(text(R.id.modelInput)) || !modelYear.equals(editing.modelYear));
        mutate(expectedScope, () -> repository.saveVehicle(expectedScope, id, body), dataChanged);
    }
    private interface Mutation { boolean run() throws IOException; }
    private void mutate(long expectedScope, Mutation action, boolean vehicleDataChanged) {
        if (!repository.current(scope)) { failed(new RegistrationRepository.Failure(401, ""), false); return; }
        setBusy(true); message("Сохраняем…"); int ticket = ++generation;
        executor.execute(() -> {
            boolean stopRequired = false;
            try {
                stopRequired = action.run();
                final boolean stopConfirmed = stopRequired;
                // Re-read authoritative state; never synthesize an approval/status/revision.
                Registration loaded = repository.load(expectedScope);
                runOnUiThread(() -> {
                    if (!active(ticket)) return;
                    if (!repository.current(scope)) { failed(new RegistrationRepository.Failure(401, ""), true); return; }
                    registration = loaded; setBusy(false); showOverview(); message(stopConfirmed ? getString(R.string.registration_stop_required) : "Сохранено. Статус анкеты: " + status(loaded.status));
                });
            } catch (IOException failure) {
                RegistrationRepository.Failure api = failure instanceof RegistrationRepository.Failure ? (RegistrationRepository.Failure) failure : null;
                boolean uncertainStop = stopRequired || (vehicleDataChanged && (api == null || api.status >= 500));
                if (uncertainStop) runOnUiThread(() -> { if (active(ticket)) { failed(failure, true); message(getString(R.string.registration_stop_uncertain)); } });
                else deliverFailure(ticket, failure, true);
            }
        });
    }
    private void deliverFailure(int ticket, IOException failure, boolean mutation) {
        runOnUiThread(() -> { if (active(ticket)) failed(failure, mutation); });
    }
    private void failed(IOException failure, boolean mutation) {
        clear(); setBusy(false);
        RegistrationRepository.Failure api = failure instanceof RegistrationRepository.Failure ? (RegistrationRepository.Failure) failure : null;
        if (api != null && api.status == 401) message("Сессия изменилась или истекла. Вернитесь на главный экран и проверьте вход.");
        else if (api != null && "active_vehicle_change_forbidden".equals(api.code)) message("Нельзя менять VIN, госномер или основание использования активного автомобиля. Обновите данные.");
        else if (api != null && api.status == 409) message("Данные или статус изменились. Обновите анкету и проверьте результат перед новым сохранением.");
        else if (api != null && api.status == 403) message("Действие недоступно. Обновите данные и проверьте статус.");
        else if (api != null && api.status == 400) message("Сервер отклонил данные. Обновите анкету и проверьте поля.");
        else message(mutation ? "Не удалось подтвердить сохранение. Обновите данные и проверьте список: изменение могло сохраниться. Повторно автоматически не отправляем."
                : "Не удалось загрузить анкету. Проверьте соединение и обновите данные.");
    }
    private boolean check(int id, int min, int max, String error) {
        boolean valid = Registration.textValid(text(id), min, max); layout(id).setError(valid ? null : error); return valid;
    }
    private EditText input(int id) { return findViewById(id); }
    private String text(int id) { return input(id).getText().toString().trim(); }
    private TextInputLayout layout(int id) { return (TextInputLayout) input(id).getParent().getParent(); }
    private void message(String value) { ((TextView) findViewById(R.id.registrationMessage)).setText(value); }
    private void select(int id, String[] values, String value, boolean enabled) {
        Spinner spinner = findViewById(id); spinner.setSelection(value == null ? 0 : Arrays.asList(values).indexOf(value) + 1); spinner.setEnabled(enabled);
    }
    private String selected(int id, String[] values) { int position = ((Spinner) findViewById(id)).getSelectedItemPosition(); return position > 0 ? values[position - 1] : null; }
    private void top() { findViewById(R.id.registrationScroll).post(() -> ((android.widget.ScrollView) findViewById(R.id.registrationScroll)).smoothScrollTo(0, 0)); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private static String status(String status) {
        switch (status) {
            case "draft": return "Черновик";
            case "submitted": return "Отправлена на проверку";
            case "under_review": return "На проверке";
            case "changes_required": return "Требуются исправления";
            case "approved": return "Одобрено";
            case "rejected": return "Отклонено";
            case "pending_review": return "Ожидает проверки";
            case "photo_check_required": return "Нужен фотоконтроль";
            case "suspended": return "Приостановлено";
            default: return "Статус недоступен";
        }
    }
}
