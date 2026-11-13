package ru.transferservis.driver.ui.support;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import ru.transferservis.driver.R;
import ru.transferservis.driver.data.model.SupportCaseDto;
import ru.transferservis.driver.data.model.SupportMessageDto;
import ru.transferservis.driver.data.repository.DriverV1Repository;
import ru.transferservis.driver.security.SecureActivity;
import ru.transferservis.driver.ui.login.LoginActivity;

public final class SupportCaseActivity
        extends SecureActivity {

    private static final String EXTRA_CASE_ID =
            "support_case_id";

    private static final String STATE_PENDING_KEY =
            "support_pending_key";

    private static final String STATE_PENDING_MESSAGE =
            "support_pending_message";

    private static final String STATE_PENDING_REVISION =
            "support_pending_revision";

    private final DriverV1Repository repository =
            new DriverV1Repository();

    private SupportMessageAdapter adapter;

    private TextView title;
    private TextView meta;
    private TextView statusMessage;
    private ProgressBar progress;
    private RecyclerView recycler;
    private EditText input;
    private Button send;
    private Button refresh;

    private String caseId;
    private SupportCaseDto currentCase;

    private String pendingKey;
    private String pendingMessage;
    private Long pendingRevision;

    private boolean sending;

    public static Intent createIntent(
            Context context,
            String caseId
    ) {

        Intent intent =
                new Intent(
                        context,
                        SupportCaseActivity.class
                );

        intent.putExtra(
                EXTRA_CASE_ID,
                caseId
        );

        return intent;
    }

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_support_case
        );

        caseId =
                getIntent()
                        .getStringExtra(
                                EXTRA_CASE_ID
                        );

        if (
                caseId == null
                        ||
                        caseId.trim().isEmpty()
        ) {

            finish();
            return;
        }

        caseId =
                caseId.trim();

        title =
                findViewById(
                        R.id.textSupportCaseTitle
                );

        meta =
                findViewById(
                        R.id.textSupportCaseMeta
                );

        statusMessage =
                findViewById(
                        R.id.textSupportCaseStatusMessage
                );

        progress =
                findViewById(
                        R.id.progressSupportCase
                );

        recycler =
                findViewById(
                        R.id.recyclerSupportMessages
                );

        input =
                findViewById(
                        R.id.editSupportMessage
                );

        send =
                findViewById(
                        R.id.buttonSendSupportMessage
                );

        refresh =
                findViewById(
                        R.id.buttonSupportCaseRefresh
                );

        adapter =
                new SupportMessageAdapter();

        recycler.setLayoutManager(
                new LinearLayoutManager(
                        this
                )
        );

        recycler.setAdapter(
                adapter
        );

        send.setOnClickListener(
                view ->
                        sendMessage()
        );

        refresh.setOnClickListener(
                view ->
                        loadCase()
        );

        if (savedInstanceState != null) {

            pendingKey =
                    savedInstanceState
                            .getString(
                                    STATE_PENDING_KEY
                            );

            pendingMessage =
                    savedInstanceState
                            .getString(
                                    STATE_PENDING_MESSAGE
                            );

            if (
                    savedInstanceState.containsKey(
                            STATE_PENDING_REVISION
                    )
            ) {

                pendingRevision =
                        savedInstanceState
                                .getLong(
                                        STATE_PENDING_REVISION
                                );
            }

            if (pendingMessage != null) {

                input.setText(
                        pendingMessage
                );

                input.setSelection(
                        input.length()
                );
            }
        }

        input.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        if (
                                sending
                                        ||
                                        pendingMessage == null
                        ) {
                            return;
                        }

                        if (
                                !pendingMessage.equals(
                                        s == null
                                                ? ""
                                                : s.toString()
                                )
                        ) {

                            clearPendingSend();
                        }
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );

        loadCase();
    }

    @Override
    protected void onSaveInstanceState(
            Bundle outState
    ) {

        outState.putString(
                STATE_PENDING_KEY,
                pendingKey
        );

        outState.putString(
                STATE_PENDING_MESSAGE,
                pendingMessage
        );

        if (pendingRevision != null) {

            outState.putLong(
                    STATE_PENDING_REVISION,
                    pendingRevision
            );
        }

        super.onSaveInstanceState(
                outState
        );
    }

    private void loadCase() {

        progress.setVisibility(
                View.VISIBLE
        );

        statusMessage.setText(
                R.string.support_loading
        );

        repository.getSupportCase(
                caseId,
                new DriverV1Repository.SupportCaseCallback() {

                    @Override
                    public void onSuccess(
                            SupportCaseDto supportCase
                    ) {

                        runOnUiThread(
                                () -> {

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    bindCase(
                                            supportCase
                                    );
                                }
                        );
                    }

                    @Override
                    public void onAuthenticationRequired() {

                        runOnUiThread(
                                () ->
                                        openLogin()
                        );
                    }

                    @Override
                    public void onConflict() {

                        runOnUiThread(
                                () -> {

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    statusMessage.setText(
                                            R.string.support_updated_reload
                                    );
                                }
                        );
                    }

                    @Override
                    public void onError(
                            int httpStatus,
                            String message
                    ) {

                        runOnUiThread(
                                () -> {

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    statusMessage.setText(
                                            safe(
                                                    message,
                                                    getString(
                                                            R.string.support_load_failed
                                                    )
                                            )
                                    );
                                }
                        );
                    }
                }
        );
    }

    private void bindCase(
            SupportCaseDto supportCase
    ) {

        currentCase =
                supportCase;

        if (supportCase == null) {

            statusMessage.setText(
                    R.string.support_load_failed
            );

            return;
        }

        title.setText(
                SupportCaseAdapter
                        .categoryLabel(
                                supportCase.getCategory()
                        )
        );

        meta.setText(
                getString(
                        R.string.support_case_meta_format,
                        SupportCaseAdapter
                                .statusLabel(
                                        supportCase.getStatus()
                                ),
                        supportCase.getOrderId() == null
                                ? getString(
                                        R.string.support_no_order
                                )
                                : supportCase.getOrderId()
                )
        );

        List<SupportMessageDto> messages =
                supportCase.getMessages();

        adapter.submit(
                messages == null
                        ? Collections.emptyList()
                        : messages
        );

        if (
                messages != null
                        &&
                        !messages.isEmpty()
        ) {

            recycler.scrollToPosition(
                    messages.size() - 1
            );
        }

        boolean closed =
                "closed".equals(
                        safe(
                                supportCase.getStatus(),
                                ""
                        )
                );

        input.setEnabled(
                !closed
        );

        send.setEnabled(
                !closed
                        &&
                        !sending
        );

        statusMessage.setText(
                closed
                        ? R.string.support_case_closed
                        : R.string.support_case_ready
        );
    }

    private void sendMessage() {

        if (
                currentCase == null
                        ||
                        sending
        ) {
            return;
        }

        if (
                "closed".equals(
                        safe(
                                currentCase.getStatus(),
                                ""
                        )
                )
        ) {
            return;
        }

        String message =
                input.getText()
                        .toString()
                        .trim();

        if (message.isEmpty()) {

            input.setError(
                    getString(
                            R.string.support_message_required
                    )
            );

            return;
        }

        if (
                currentCase.getRevision()
                        == null
                        ||
                        currentCase.getRevision()
                                < 1
        ) {

            statusMessage.setText(
                    R.string.support_revision_invalid
            );

            return;
        }

        if (pendingKey == null) {

            pendingKey =
                    DriverV1Repository
                            .newMutationKey(
                                    "support-message"
                            );

            pendingMessage =
                    message;

            pendingRevision =
                    currentCase.getRevision();
        }

        sending =
                true;

        send.setEnabled(
                false
        );

        progress.setVisibility(
                View.VISIBLE
        );

        statusMessage.setText(
                R.string.support_sending
        );

        repository.sendSupportMessage(
                caseId,
                pendingMessage,
                pendingRevision,
                pendingKey,
                new DriverV1Repository.SupportCaseCallback() {

                    @Override
                    public void onSuccess(
                            SupportCaseDto supportCase
                    ) {

                        runOnUiThread(
                                () -> {

                                    sending =
                                            false;

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    clearPendingSend();

                                    input.setText(
                                            ""
                                    );

                                    bindCase(
                                            supportCase
                                    );
                                }
                        );
                    }

                    @Override
                    public void onAuthenticationRequired() {

                        runOnUiThread(
                                () ->
                                        openLogin()
                        );
                    }

                    @Override
                    public void onConflict() {

                        runOnUiThread(
                                () -> {

                                    sending =
                                            false;

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    clearPendingSend();

                                    statusMessage.setText(
                                            R.string.support_updated_reload
                                    );

                                    loadCase();
                                }
                        );
                    }

                    @Override
                    public void onError(
                            int httpStatus,
                            String message
                    ) {

                        runOnUiThread(
                                () -> {

                                    sending =
                                            false;

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    if (httpStatus != 0) {

                                        clearPendingSend();
                                    }

                                    send.setEnabled(
                                            true
                                    );

                                    statusMessage.setText(
                                            safe(
                                                    message,
                                                    getString(
                                                            R.string.support_send_failed
                                                    )
                                            )
                                    );
                                }
                        );
                    }
                }
        );
    }

    private void clearPendingSend() {

        pendingKey =
                null;

        pendingMessage =
                null;

        pendingRevision =
                null;
    }

    private void openLogin() {


        Intent intent =
                new Intent(
                        this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(
                intent
        );

        finish();
    }

    private String safe(
            String value,
            String fallback
    ) {

        if (
                value == null
                        ||
                        value.trim().isEmpty()
        ) {

            return fallback;
        }

        return value.trim();
    }
}
