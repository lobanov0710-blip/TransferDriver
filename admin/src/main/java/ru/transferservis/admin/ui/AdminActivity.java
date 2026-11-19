package ru.transferservis.admin.ui;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import ru.transferservis.admin.R;
import ru.transferservis.admin.data.RegistrationRow;

public final class AdminActivity extends AppCompatActivity {
    private AdminViewModel model;
    @Override protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_admin);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        model = new ViewModelProvider(this).get(AdminViewModel.class);
        EditText login = findViewById(R.id.login), password = findViewById(R.id.password);
        findViewById(R.id.signIn).setOnClickListener(v -> {
            String secret = password.getText().toString(); password.setText("");
            model.login(login.getText().toString(), secret);
        });
        findViewById(R.id.retry).setOnClickListener(v -> model.restore());
        findViewById(R.id.queue).setOnClickListener(v -> model.queue());
        findViewById(R.id.logout).setOnClickListener(v -> { login.setText(""); password.setText(""); model.logout(); });
        model.state().observe(this, this::render);
        model.initialize();
    }
    private void render(AdminViewModel.State state) {
        boolean authenticated = state.access != null;
        findViewById(R.id.progress).setVisibility(state.loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.loginForm).setVisibility(!authenticated && !state.loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.profile).setVisibility(authenticated ? View.VISIBLE : View.GONE);
        findViewById(R.id.logout).setVisibility(authenticated || state.loading ? View.VISIBLE : View.GONE);
        TextView message = findViewById(R.id.message);
        message.setText(state.error == 0 ? "" : getString(state.error));
        message.setVisibility(state.error == 0 ? View.GONE : View.VISIBLE);
        TextView role = findViewById(R.id.role), security = findViewById(R.id.security);
        role.setText(authenticated ? getString(state.access.isOwner() ? R.string.owner : R.string.dispatcher) : "");
        security.setText(!authenticated ? "" : getString(state.access.isOwner() ? R.string.owner_security_pending : R.string.server_permissions));
        Button queue = findViewById(R.id.queue);
        queue.setEnabled(authenticated && state.access.canReadRegistrations() && !state.loading);
        TextView registrations = findViewById(R.id.registrations);
        StringBuilder content = new StringBuilder();
        for (RegistrationRow row : state.registrations) {
            int status = "submitted".equals(row.status) ? R.string.submitted
                    : "under_review".equals(row.status) ? R.string.under_review : R.string.changes_required;
            content.append(getString(R.string.registration_row, limited(row.fullName), limited(row.region), getString(status)));
        }
        registrations.setText(state.queueLoaded && state.registrations.isEmpty() ? getString(R.string.queue_empty) : content.toString());
        registrations.setVisibility(state.queueLoaded ? View.VISIBLE : View.GONE);
    }
    private String limited(String text) { return text.length() > 160 ? text.substring(0, 160) : text; }
}
