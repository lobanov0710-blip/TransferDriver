package ru.transferservis.driver.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import ru.transferservis.driver.R;
import ru.transferservis.driver.data.model.StaffDto;
import ru.transferservis.driver.data.model.StaffLoginResponse;
import ru.transferservis.driver.data.session.SecureSessionStore;
import ru.transferservis.driver.ui.main.DriverMainActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editLogin;
    private EditText editPassword;

    private Button buttonLogin;

    private ProgressBar progressLogin;

    private TextView textLoginError;

    private LoginViewModel viewModel;

    private SecureSessionStore sessionStore;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        sessionStore =
                new SecureSessionStore(
                        this
                );

        if (
                sessionStore
                        .hasRefreshableDriverSession()
        ) {

            openDriverMain();

            return;
        }

        setContentView(
                R.layout.activity_login
        );

        initViews();

        initViewModel();

        observeViewModel();

        buttonLogin.setOnClickListener(
                view -> submitLogin()
        );
    }

    private void initViews() {

        editLogin =
                findViewById(
                        R.id.editLogin
                );

        editPassword =
                findViewById(
                        R.id.editPassword
                );

        buttonLogin =
                findViewById(
                        R.id.buttonLogin
                );

        progressLogin =
                findViewById(
                        R.id.progressLogin
                );

        textLoginError =
                findViewById(
                        R.id.textLoginError
                );
    }

    private void initViewModel() {

        viewModel =
                new ViewModelProvider(
                        this
                )
                        .get(
                                LoginViewModel.class
                        );
    }

    private void observeViewModel() {

        viewModel
                .getLoading()
                .observe(
                        this,
                        this::showLoading
                );

        viewModel
                .getLoginResult()
                .observe(
                        this,
                        result -> {

                            if (result == null) {
                                return;
                            }

                            if (!result.isSuccess()) {

                                showLoginError(
                                        result.getMessage()
                                );

                                return;
                            }

                            StaffLoginResponse response =
                                    result.getResponse();

                            if (response == null) {

                                showLoginError(
                                        getString(
                                                R.string.error_invalid_server_response
                                        )
                                );

                                return;
                            }

                            StaffDto staff =
                                    response.getStaff();

                            if (staff == null) {

                                showLoginError(
                                        getString(
                                                R.string.error_invalid_server_response
                                        )
                                );

                                return;
                            }

                            if (
                                    !sessionStore.save(
                                            response
                                    )
                            ) {

                                showLoginError(
                                        getString(
                                                R.string.error_session_save
                                        )
                                );

                                return;
                            }

                            editPassword.setText(
                                    ""
                            );

                            openDriverMain();
                        }
                );
    }

    private void submitLogin() {

        clearErrors();

        String login =
                editLogin
                        .getText()
                        .toString()
                        .trim();

        String password =
                editPassword
                        .getText()
                        .toString();

        boolean valid =
                true;

        if (
                TextUtils.isEmpty(
                        login
                )
        ) {

            editLogin.setError(
                    getString(
                            R.string.error_login_required
                    )
            );

            valid =
                    false;
        }

        if (
                TextUtils.isEmpty(
                        password
                )
        ) {

            editPassword.setError(
                    getString(
                            R.string.error_password_required
                    )
            );

            valid =
                    false;
        }

        if (!valid) {
            return;
        }

        viewModel.login(
                login,
                password
        );
    }

    private void clearErrors() {

        editLogin.setError(
                null
        );

        editPassword.setError(
                null
        );

        textLoginError.setText(
                ""
        );

        textLoginError.setVisibility(
                View.GONE
        );
    }

    private void showLoginError(
            String message
    ) {

        String safeMessage =
                message;

        if (
                safeMessage == null
                        ||
                        safeMessage.trim().isEmpty()
        ) {

            safeMessage =
                    getString(
                            R.string.error_login_failed
                    );
        }

        textLoginError.setText(
                safeMessage
        );

        textLoginError.setVisibility(
                View.VISIBLE
        );

        editPassword.setText(
                ""
        );
    }

    private void showLoading(
            Boolean loading
    ) {

        boolean isLoading =
                Boolean.TRUE.equals(
                        loading
                );

        progressLogin.setVisibility(
                isLoading
                        ? View.VISIBLE
                        : View.GONE
        );

        buttonLogin.setEnabled(
                !isLoading
        );

        editLogin.setEnabled(
                !isLoading
        );

        editPassword.setEnabled(
                !isLoading
        );
    }

    private void openDriverMain() {

        Intent intent =
                new Intent(
                        LoginActivity.this,
                        DriverMainActivity.class
                );

        startActivity(
                intent
        );

        finish();
    }
}