package ru.transferservis.driver.ui.support;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import ru.transferservis.driver.R;
import ru.transferservis.driver.data.model.SupportCaseDto;
import ru.transferservis.driver.data.repository.DriverV1Repository;
import ru.transferservis.driver.data.repository.SupportCasesGateway;
import ru.transferservis.driver.data.session.SecureSessionStore;
import ru.transferservis.driver.security.SecureActivity;
import ru.transferservis.driver.ui.login.LoginActivity;

public final class SupportCasesActivity
        extends SecureActivity {

    private static final String STATE_PENDING_KEY =
            "pending_support_key";

    private static final String STATE_PENDING_CATEGORY =
            "pending_support_category";

    private static final String[] CATEGORY_VALUES = {
            "passenger_no_show",
            "delay",
            "vehicle",
            "route",
            "unable_to_complete",
            "reassignment",
            "conflict",
            "other"
    };

    private SupportCasesGateway repository;
    private int loadGeneration;
    private SupportCaseAdapter adapter;

    private TextView status;
    private TextView empty;
    private ProgressBar progress;
    private RecyclerView recycler;
    private Button create;
    private Button retry;

    private String pendingCreateKey;
    private String pendingCategory;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_support_cases
        );

        repository = SupportCasesGatewayFactory.create();

        status =
                findViewById(
                        R.id.textSupportCasesStatus
                );

        empty =
                findViewById(
                        R.id.textSupportCasesEmpty
                );

        progress =
                findViewById(
                        R.id.progressSupportCases
                );

        recycler =
                findViewById(
                        R.id.recyclerSupportCases
                );

        create =
                findViewById(
                        R.id.buttonCreateSupportCase
                );

        retry =
                findViewById(
                        R.id.buttonSupportCasesRetry
                );

        adapter =
                new SupportCaseAdapter(
                        this::openCase
                );

        recycler.setLayoutManager(
                new LinearLayoutManager(
                        this
                )
        );

        recycler.setAdapter(
                adapter
        );

        create.setOnClickListener(
                view -> {

                    if (
                            pendingCreateKey != null
                                    &&
                                    pendingCategory != null
                    ) {

                        createCase(
                                pendingCategory,
                                pendingCreateKey
                        );

                    }
                    else {

                        showCategoryDialog();
                    }
                }
        );

        retry.setOnClickListener(
                view ->
                        loadCases()
        );

        if (savedInstanceState != null) {

            pendingCreateKey =
                    savedInstanceState
                            .getString(
                                    STATE_PENDING_KEY
                            );

            pendingCategory =
                    savedInstanceState
                            .getString(
                                    STATE_PENDING_CATEGORY
                            );

            updateCreateButton();
        }

        loadCases();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (
                recycler.getVisibility()
                        == View.VISIBLE
        ) {

            loadCases();
        }
    }

    @Override
    protected void onSaveInstanceState(
            Bundle outState
    ) {

        outState.putString(
                STATE_PENDING_KEY,
                pendingCreateKey
        );

        outState.putString(
                STATE_PENDING_CATEGORY,
                pendingCategory
        );

        super.onSaveInstanceState(
                outState
        );
    }

    private void loadCases() {

        final int generation = ++loadGeneration;

        // Do not expose cached support cases while ownership is revalidated.
        adapter.submit(null);
        recycler.setVisibility(View.GONE);
        empty.setVisibility(View.GONE);

        progress.setVisibility(
                View.VISIBLE
        );

        retry.setVisibility(
                View.GONE
        );

        status.setText(
                R.string.support_loading
        );

        repository.getSupportCases(
                100,
                new DriverV1Repository.SupportCasesCallback() {

                    @Override
                    public void onSuccess(
                            List<SupportCaseDto> cases
                    ) {

                        runOnUiThread(
                                () -> {

                                    if (generation != loadGeneration || isFinishing()) {
                                        return;
                                    }

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    adapter.submit(
                                            cases
                                    );

                                    boolean hasItems =
                                            cases != null
                                                    &&
                                                    !cases.isEmpty();

                                    recycler.setVisibility(
                                            hasItems
                                                    ? View.VISIBLE
                                                    : View.GONE
                                    );

                                    empty.setVisibility(
                                            hasItems
                                                    ? View.GONE
                                                    : View.VISIBLE
                                    );

                                    status.setText(
                                            hasItems
                                                    ? R.string.support_ready
                                                    : R.string.support_empty_short
                                    );
                                }
                        );
                    }

                    @Override
                    public void onAuthenticationRequired() {

                        runOnUiThread(
                                () -> {
                                    if (generation == loadGeneration && !isFinishing()) {
                                        openLogin();
                                    }
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

                                    if (generation != loadGeneration || isFinishing()) {
                                        return;
                                    }

                                    adapter.submit(null);
                                    recycler.setVisibility(View.GONE);
                                    empty.setVisibility(View.GONE);

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    retry.setVisibility(
                                            View.VISIBLE
                                    );

                                    status.setText(
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

    private void showCategoryDialog() {

        String[] labels = {
                getString(
                        R.string.support_category_passenger_no_show
                ),
                getString(
                        R.string.support_category_delay
                ),
                getString(
                        R.string.support_category_vehicle
                ),
                getString(
                        R.string.support_category_route
                ),
                getString(
                        R.string.support_category_unable
                ),
                getString(
                        R.string.support_category_reassignment
                ),
                getString(
                        R.string.support_category_conflict
                ),
                getString(
                        R.string.support_category_other
                )
        };

        AlertDialog dialog = new MaterialAlertDialogBuilder(
                this
        )
                .setTitle(
                        R.string.support_choose_category
                )
                .setItems(
                        labels,
                        (
                                dialog,
                                which
                        ) -> {

                            if (
                                    which < 0
                                            ||
                                            which >= CATEGORY_VALUES.length
                            ) {
                                return;
                            }

                            pendingCategory =
                                    CATEGORY_VALUES[which];

                            pendingCreateKey =
                                    DriverV1Repository
                                            .newMutationKey(
                                                    "support-case"
                                            );

                            createCase(
                                    pendingCategory,
                                    pendingCreateKey
                            );
                        }
                )
                .setNegativeButton(
                        android.R.string.cancel,
                        null
                )
                .create();

        // A dialog has its own Window; set FLAG_SECURE before showing it.
        if (dialog.getWindow() != null) {
            dialog.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SECURE
            );
        }
        dialog.show();
    }

    private void createCase(
            String category,
            String idempotencyKey
    ) {

        create.setEnabled(
                false
        );

        progress.setVisibility(
                View.VISIBLE
        );

        status.setText(
                R.string.support_creating
        );

        repository.createSupportCase(
                category,
                null,
                idempotencyKey,
                new DriverV1Repository.SupportCaseCallback() {

                    @Override
                    public void onSuccess(
                            SupportCaseDto supportCase
                    ) {

                        runOnUiThread(
                                () -> {

                                    pendingCreateKey =
                                            null;

                                    pendingCategory =
                                            null;

                                    updateCreateButton();

                                    create.setEnabled(
                                            true
                                    );

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    openCase(
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

                                    pendingCreateKey =
                                            null;

                                    pendingCategory =
                                            null;

                                    updateCreateButton();

                                    create.setEnabled(
                                            true
                                    );

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    status.setText(
                                            R.string.support_create_conflict
                                    );

                                    loadCases();
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

                                    if (httpStatus != 0) {

                                        pendingCreateKey =
                                                null;

                                        pendingCategory =
                                                null;
                                    }

                                    updateCreateButton();

                                    create.setEnabled(
                                            true
                                    );

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    status.setText(
                                            safe(
                                                    message,
                                                    getString(
                                                            R.string.support_create_failed
                                                    )
                                            )
                                    );
                                }
                        );
                    }
                }
        );
    }

    private void updateCreateButton() {

        create.setText(
                pendingCreateKey == null
                        ? R.string.support_create
                        : R.string.support_create_retry
        );
    }

    private void openCase(
            SupportCaseDto supportCase
    ) {

        if (
                supportCase == null
                        ||
                        supportCase.getId() == null
                        ||
                        supportCase.getId()
                                .trim()
                                .isEmpty()
        ) {
            return;
        }

        startActivity(
                SupportCaseActivity.createIntent(
                        this,
                        supportCase.getId()
                )
        );
    }

    private void openLogin() {

        new SecureSessionStore(
                getApplicationContext()
        )
                .clear();

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
