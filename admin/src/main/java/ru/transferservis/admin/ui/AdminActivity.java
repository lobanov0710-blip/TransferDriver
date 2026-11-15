package ru.transferservis.admin.ui;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import ru.transferservis.admin.R;
import ru.transferservis.admin.data.RegistrationRow;

public final class AdminActivity extends AppCompatActivity {
    private AdminViewModel model;
    private ReviewPanel reviews;
    @Override protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_admin);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        model = new ViewModelProvider(this).get(AdminViewModel.class);
        reviews=new ReviewPanel(this,model);
        Spinner filter=findViewById(R.id.queueFilter);
        filter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,getResources().getStringArray(R.array.queue_filters)));
        findViewById(R.id.queueItems).setSaveFromParentEnabled(false);
        EditText login = findViewById(R.id.login), password = findViewById(R.id.password);
        findViewById(R.id.signIn).setOnClickListener(v -> {
            String secret = password.getText().toString(); password.setText("");
            model.login(login.getText().toString(), secret);
        });
        findViewById(R.id.retry).setOnClickListener(v -> model.restore());
        findViewById(R.id.queue).setOnClickListener(v -> model.queue(new String[]{"","submitted","under_review","changes_required","approved","rejected"}[filter.getSelectedItemPosition()]));
        findViewById(R.id.logout).setOnClickListener(v -> { login.setText(""); password.setText(""); model.logout(); });
        findViewById(R.id.verifyTotp).setOnClickListener(v -> model.verify(take(R.id.totpCode), false));
        findViewById(R.id.verifyRecovery).setOnClickListener(v -> model.verify(take(R.id.recoveryCode), true));
        findViewById(R.id.startEnrollment).setOnClickListener(v -> model.startEnrollment(take(R.id.bootstrapCode)));
        findViewById(R.id.confirmEnrollment).setOnClickListener(v -> model.confirmEnrollment(take(R.id.enrollmentCode)));
        findViewById(R.id.recoverySaved).setOnClickListener(v -> model.acknowledgeRecoveryCodes());
        model.state().observe(this, this::render);
        model.initialize();
    }
    private void render(AdminViewModel.State state) {
        reviews.render(state);
        boolean authenticated = state.access != null;
        findViewById(R.id.progress).setVisibility(state.loading ? View.VISIBLE : View.GONE);
        boolean securityFlow = state.hasTransientSecurityData();
        findViewById(R.id.loginForm).setVisibility(!authenticated && !state.loading && !securityFlow ? View.VISIBLE : View.GONE);
        findViewById(R.id.mfaForm).setVisibility(!state.loading && state.challenge != null
                && !state.challenge.enrollmentRequired ? View.VISIBLE : View.GONE);
        findViewById(R.id.bootstrapForm).setVisibility(!state.loading && state.challenge != null
                && state.challenge.enrollmentRequired && state.enrollment == null ? View.VISIBLE : View.GONE);
        findViewById(R.id.enrollmentForm).setVisibility(!state.loading && state.enrollment != null ? View.VISIBLE : View.GONE);
        findViewById(R.id.recoveryForm).setVisibility(!state.loading && !state.recoveryCodes.isEmpty() ? View.VISIBLE : View.GONE);
        ((TextView)findViewById(R.id.enrollmentSecret)).setText(state.enrollment == null ? "" : state.enrollment.secret);
        ((TextView)findViewById(R.id.enrollmentPrompt)).setText(state.enrollment!=null&&state.enrollment.secret.isEmpty()?R.string.enrollment_confirm_after_switch:R.string.enrollment_prompt);
        ((TextView)findViewById(R.id.backupCodes)).setText(android.text.TextUtils.join("\n\n", state.recoveryCodes));
        findViewById(R.id.profile).setVisibility(authenticated ? View.VISIBLE : View.GONE);
        findViewById(R.id.logout).setVisibility(authenticated || state.loading || securityFlow ? View.VISIBLE : View.GONE);
        TextView message = findViewById(R.id.message);
        message.setText(state.error == 0 ? "" : getString(state.error));
        message.setVisibility(state.error == 0 ? View.GONE : View.VISIBLE);
        TextView role = findViewById(R.id.role), security = findViewById(R.id.security);
        role.setText(authenticated ? getString(state.access.isOwner() ? R.string.owner : R.string.dispatcher) : "");
        security.setText(!authenticated ? "" : getString(state.access.ownerPhoneTest?R.string.owner_phone_test_notice:state.access.isOwner() ? (state.access.hasVerifiedMfa() ? R.string.owner_mfa_verified : R.string.owner_security_pending) : R.string.server_permissions));
        Button queue = findViewById(R.id.queue);
        queue.setEnabled(authenticated && state.access.canReadRegistrations() && !state.loading);
        TextView registrations = findViewById(R.id.registrations);
        LinearLayout rows=findViewById(R.id.queueItems);rows.removeAllViews();
        for (RegistrationRow row : state.registrations) {
            String label=getString(R.string.registration_row,limited(row.fullName),limited(row.region),reviews.status(row.status));
            Button entry=new Button(this);entry.setText(label);entry.setSaveEnabled(false);
            entry.setEnabled(authenticated&&state.access.canReadReview()&&!state.loading);
            entry.setOnClickListener(v->model.openReview(row.driverId));rows.addView(entry);
        }
        registrations.setText(state.queueLoaded && state.registrations.isEmpty() ? getString(R.string.queue_empty) : "");
        registrations.setVisibility(state.queueLoaded ? View.VISIBLE : View.GONE);
    }
    private String take(int id) {
        EditText field = findViewById(id); String value = field.getText().toString(); field.setText(""); return value;
    }
    @Override protected void onStart(){super.onStart();if(model!=null)model.expireSecurityFlow();}
    @Override protected void onStop() {
        if(reviews!=null)reviews.clear();
        ((LinearLayout)findViewById(R.id.queueItems)).removeAllViews();
        ((TextView)findViewById(R.id.registrations)).setText("");
        for (int id : new int[]{R.id.password,R.id.totpCode,R.id.recoveryCode,R.id.bootstrapCode,R.id.enrollmentCode})
            ((EditText)findViewById(id)).setText("");
        ((TextView)findViewById(R.id.enrollmentSecret)).setText("");
        ((TextView)findViewById(R.id.backupCodes)).setText("");
        if (!isChangingConfigurations() && model != null) model.hideSecurityFlow();
        super.onStop();
    }
    private String limited(String text) { return text.length() > 160 ? text.substring(0, 160) : text; }
}
