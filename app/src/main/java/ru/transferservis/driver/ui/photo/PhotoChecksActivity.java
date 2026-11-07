package ru.transferservis.driver.ui.photo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.transferservis.driver.R;
import ru.transferservis.driver.data.model.DriverPhotoCheckDto;
import ru.transferservis.driver.data.repository.DriverV1Repository;
import ru.transferservis.driver.data.session.SecureSessionStore;
import ru.transferservis.driver.security.SecureActivity;
import ru.transferservis.driver.ui.login.LoginActivity;

public final class PhotoChecksActivity
        extends SecureActivity {

    private final DriverV1Repository repository =
            new DriverV1Repository();

    private PhotoCheckAdapter adapter;

    private TextView status;
    private TextView empty;
    private ProgressBar progress;
    private RecyclerView recycler;
    private Button retry;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_photo_checks
        );

        status =
                findViewById(
                        R.id.textPhotoChecksStatus
                );

        empty =
                findViewById(
                        R.id.textPhotoChecksEmpty
                );

        progress =
                findViewById(
                        R.id.progressPhotoChecks
                );

        recycler =
                findViewById(
                        R.id.recyclerPhotoChecks
                );

        retry =
                findViewById(
                        R.id.buttonPhotoChecksRetry
                );

        adapter =
                new PhotoCheckAdapter(
                        this::openCheck
                );

        recycler.setLayoutManager(
                new LinearLayoutManager(
                        this
                )
        );

        recycler.setAdapter(
                adapter
        );

        retry.setOnClickListener(
                view ->
                        loadChecks()
        );

        loadChecks();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (
                recycler.getVisibility()
                        == View.VISIBLE
        ) {

            loadChecks();
        }
    }

    private void loadChecks() {

        progress.setVisibility(
                View.VISIBLE
        );

        retry.setVisibility(
                View.GONE
        );

        status.setText(
                R.string.photo_control_loading
        );

        repository.getPhotoChecks(
                new DriverV1Repository.PhotoChecksCallback() {

                    @Override
                    public void onSuccess(
                            List<DriverPhotoCheckDto> checks
                    ) {

                        runOnUiThread(
                                () -> {

                                    progress.setVisibility(
                                            View.GONE
                                    );

                                    adapter.submit(
                                            checks
                                    );

                                    boolean hasItems =
                                            checks != null
                                                    &&
                                                    !checks.isEmpty();

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
                                                    ? R.string.photo_control_ready
                                                    : R.string.photo_control_empty_short
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
                    public void onError(
                            int httpStatus,
                            String message
                    ) {

                        runOnUiThread(
                                () -> {

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
                                                            R.string.photo_control_load_failed
                                                    )
                                            )
                                    );
                                }
                        );
                    }
                }
        );
    }

    private void openCheck(
            DriverPhotoCheckDto check
    ) {

        if (
                check == null
                        ||
                        check.getId() == null
                        ||
                        check.getId()
                                .trim()
                                .isEmpty()
        ) {

            return;
        }

        startActivity(
                PhotoCheckActivity.createIntent(
                        this,
                        check.getId()
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
