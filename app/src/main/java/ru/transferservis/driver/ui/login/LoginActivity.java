package ru.transferservis.driver.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.FirebaseException;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

import ru.transferservis.driver.R;
import ru.transferservis.driver.data.model.StaffDto;
import ru.transferservis.driver.data.model.StaffLoginResponse;
import ru.transferservis.driver.data.session.SecureSessionStore;
import ru.transferservis.driver.notifications.PushDeviceRegistrar;
import ru.transferservis.driver.notifications.PushTokenStore;
import ru.transferservis.driver.security.DriverPhoneNormalizer;
import ru.transferservis.driver.security.SecureActivity;
import ru.transferservis.driver.ui.main.DriverMainActivity;

public final class LoginActivity
        extends SecureActivity {

    private static final String STATE_VERIFICATION_ID =
            "driver_phone_verification_id";

    private static final String STATE_PHONE =
            "driver_phone_value";

    private EditText editPhone;
    private EditText editSmsCode;

    private Button buttonSendCode;
    private Button buttonVerifyCode;
    private Button buttonResendCode;

    private ProgressBar progressLogin;

    private TextView textLoginError;

    private LoginViewModel viewModel;

    private SecureSessionStore sessionStore;

    private FirebaseAuth firebaseAuth;

    private FirebaseAppCheck firebaseAppCheck;

    private PhoneAuthProvider.ForceResendingToken
            forceResendingToken;

    private String verificationId;

    private boolean localBusy;

    private final PhoneAuthProvider
            .OnVerificationStateChangedCallbacks
            verificationCallbacks =
            new PhoneAuthProvider
                    .OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(
                        @NonNull PhoneAuthCredential credential
                ) {

                    editSmsCode.setText(
                            ""
                    );

                    completeFirebaseSignIn(
                            credential
                    );
                }

                @Override
                public void onVerificationFailed(
                        @NonNull FirebaseException exception
                ) {

                    localBusy =
                            false;

                    updateBusyState();

                    if (
                            exception instanceof
                                    FirebaseTooManyRequestsException
                    ) {

                        showLoginError(
                                getString(
                                        R.string.error_phone_rate_limited
                                )
                        );

                        return;
                    }

                    if (
                            exception instanceof
                                    FirebaseAuthInvalidCredentialsException
                    ) {

                        editPhone.setError(
                                getString(
                                        R.string.error_phone_invalid
                                )
                        );

                        showLoginError(
                                getString(
                                        R.string.error_phone_invalid
                                )
                        );

                        return;
                    }

                    showLoginError(
                            getString(
                                    R.string.error_phone_verification_failed
                            )
                    );
                }

                @Override
                public void onCodeSent(
                        @NonNull String newVerificationId,
                        @NonNull PhoneAuthProvider
                                .ForceResendingToken token
                ) {

                    verificationId =
                            newVerificationId;

                    forceResendingToken =
                            token;

                    localBusy =
                            false;

                    showCodeStep();

                    updateBusyState();

                    textLoginError.setVisibility(
                            View.GONE
                    );

                    textLoginError.setText(
                            ""
                    );
                }
            };


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

        firebaseAuth =
                FirebaseAuth.getInstance();

        firebaseAppCheck =
                FirebaseAppCheck.getInstance();

        initViews();

        initViewModel();

        observeViewModel();

        if (savedInstanceState != null) {

            verificationId =
                    clean(
                            savedInstanceState
                                    .getString(
                                            STATE_VERIFICATION_ID
                                    )
                    );

            String restoredPhone =
                    clean(
                            savedInstanceState
                                    .getString(
                                            STATE_PHONE
                                    )
                    );

            if (!restoredPhone.isEmpty()) {

                editPhone.setText(
                        restoredPhone
                );
            }

            if (!clean(
                    verificationId
            ).isEmpty()) {

                showCodeStep();
            }
        }

        buttonSendCode.setOnClickListener(
                view ->
                        startPhoneVerification(
                                false
                        )
        );

        buttonResendCode.setOnClickListener(
                view ->
                        startPhoneVerification(
                                true
                        )
        );

        buttonVerifyCode.setOnClickListener(
                view ->
                        verifySmsCode()
        );

        updateBusyState();
    }


    @Override
    protected void onSaveInstanceState(
            @NonNull Bundle outState
    ) {

        /*
         * verificationId is an opaque Firebase
         * workflow identifier. SMS codes and
         * Firebase/App Check tokens are never
         * written into this Bundle.
         */
        outState.putString(
                STATE_VERIFICATION_ID,
                verificationId
        );

        outState.putString(
                STATE_PHONE,
                editPhone == null
                        ? null
                        : editPhone
                                .getText()
                                .toString()
        );

        super.onSaveInstanceState(
                outState
        );
    }


    private void initViews() {

        editPhone =
                findViewById(
                        R.id.editPhone
                );

        editSmsCode =
                findViewById(
                        R.id.editSmsCode
                );

        buttonSendCode =
                findViewById(
                        R.id.buttonSendCode
                );

        buttonVerifyCode =
                findViewById(
                        R.id.buttonVerifyCode
                );

        buttonResendCode =
                findViewById(
                        R.id.buttonResendCode
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
                        loading ->
                                updateBusyState()
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

                            if (
                                    response == null
                                            ||
                                            response.getStaff() == null
                            ) {

                                showLoginError(
                                        getString(
                                                R.string.error_invalid_server_response
                                        )
                                );

                                return;
                            }

                            StaffDto staff =
                                    response.getStaff();

                            if (
                                    staff.getRole() == null
                                            ||
                                            !"driver".equalsIgnoreCase(
                                                    staff.getRole()
                                                            .trim()
                                            )
                            ) {

                                showLoginError(
                                        getString(
                                                R.string.error_driver_role_required
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

                            editSmsCode.setText(
                                    ""
                            );

                            verificationId =
                                    null;

                            forceResendingToken =
                                    null;

                            new PushDeviceRegistrar(
                                    LoginActivity.this
                            )
                                    .registerIfNeeded();

                            openDriverMain();
                        }
                );
    }


    private void startPhoneVerification(
            boolean forceResend
    ) {

        clearErrors();

        String normalizedPhone =
                DriverPhoneNormalizer
                        .normalize(
                                editPhone
                                        .getText()
                                        .toString()
                        );

        if (normalizedPhone == null) {

            editPhone.setError(
                    getString(
                            R.string.error_phone_invalid
                    )
            );

            return;
        }

        editPhone.setText(
                normalizedPhone
        );

        if (
                forceResend
                        &&
                        forceResendingToken == null
        ) {

            showLoginError(
                    getString(
                            R.string.error_resend_unavailable
                    )
            );

            return;
        }

        if (!forceResend) {

            firebaseAuth.signOut();

            verificationId =
                    null;

            forceResendingToken =
                    null;

            editSmsCode.setText(
                    ""
            );
        }

        localBusy =
                true;

        updateBusyState();

        PhoneAuthOptions.Builder builder =
                PhoneAuthOptions
                        .newBuilder(
                                firebaseAuth
                        )
                        .setPhoneNumber(
                                normalizedPhone
                        )
                        .setTimeout(
                                60L,
                                TimeUnit.SECONDS
                        )
                        .setActivity(
                                this
                        )
                        .setCallbacks(
                                verificationCallbacks
                        );

        if (forceResend) {

            builder.setForceResendingToken(
                    forceResendingToken
            );
        }

        PhoneAuthProvider
                .verifyPhoneNumber(
                        builder.build()
                );
    }


    private void verifySmsCode() {

        clearErrors();

        String safeVerificationId =
                clean(
                        verificationId
                );

        String code =
                editSmsCode
                        .getText()
                        .toString()
                        .trim();

        if (safeVerificationId.isEmpty()) {

            showLoginError(
                    getString(
                            R.string.error_sms_session_missing
                    )
            );

            return;
        }

        if (
                code.length() < 4
                        ||
                        code.length() > 8
                        ||
                        !TextUtils.isDigitsOnly(
                                code
                        )
        ) {

            editSmsCode.setError(
                    getString(
                            R.string.error_sms_code_invalid
                    )
            );

            return;
        }

        PhoneAuthCredential credential =
                PhoneAuthProvider
                        .getCredential(
                                safeVerificationId,
                                code
                        );

        /*
         * Drop visible SMS code immediately.
         * Firebase owns the credential object.
         */
        editSmsCode.setText(
                ""
        );

        completeFirebaseSignIn(
                credential
        );
    }


    private void completeFirebaseSignIn(
            PhoneAuthCredential credential
    ) {

        if (credential == null) {
            return;
        }

        localBusy =
                true;

        updateBusyState();

        firebaseAuth
                .signInWithCredential(
                        credential
                )
                .addOnCompleteListener(
                        this,
                        task -> {

                            if (
                                    !task.isSuccessful()
                                            ||
                                            task.getResult() == null
                                            ||
                                            task.getResult()
                                                    .getUser() == null
                            ) {

                                localBusy =
                                        false;

                                updateBusyState();

                                showLoginError(
                                        getString(
                                                R.string.error_sms_code_rejected
                                        )
                                );

                                return;
                            }

                            FirebaseUser user =
                                    task.getResult()
                                            .getUser();

                            requestFirebaseIdToken(
                                    user
                            );
                        }
                );
    }


    private void requestFirebaseIdToken(
            FirebaseUser user
    ) {

        if (user == null) {

            localBusy =
                    false;

            updateBusyState();

            showLoginError(
                    getString(
                            R.string.error_phone_verification_failed
                    )
            );

            return;
        }

        user
                .getIdToken(
                        true
                )
                .addOnCompleteListener(
                        this,
                        task -> {

                            if (
                                    !task.isSuccessful()
                                            ||
                                            task.getResult() == null
                            ) {

                                localBusy =
                                        false;

                                updateBusyState();

                                showLoginError(
                                        getString(
                                                R.string.error_phone_verification_failed
                                        )
                                );

                                return;
                            }

                            String firebaseIdToken =
                                    clean(
                                            task.getResult()
                                                    .getToken()
                                    );

                            if (firebaseIdToken.isEmpty()) {

                                localBusy =
                                        false;

                                updateBusyState();

                                showLoginError(
                                        getString(
                                                R.string.error_phone_verification_failed
                                        )
                                );

                                return;
                            }

                            requestAppCheckToken(
                                    firebaseIdToken
                            );
                        }
                );
    }


    private void requestAppCheckToken(
            String firebaseIdToken
    ) {

        firebaseAppCheck
                .getAppCheckToken(
                        false
                )
                .addOnCompleteListener(
                        this,
                        task -> {

                            if (
                                    !task.isSuccessful()
                                            ||
                                            task.getResult() == null
                            ) {

                                localBusy =
                                        false;

                                updateBusyState();

                                showLoginError(
                                        getString(
                                                R.string.error_app_check_failed
                                        )
                                );

                                return;
                            }

                            String appCheckToken =
                                    clean(
                                            task.getResult()
                                                    .getToken()
                                    );

                            if (appCheckToken.isEmpty()) {

                                localBusy =
                                        false;

                                updateBusyState();

                                showLoginError(
                                        getString(
                                                R.string.error_app_check_failed
                                        )
                                );

                                return;
                            }

                            String installationId =
                                    new PushTokenStore(
                                            getApplicationContext()
                                    )
                                            .getOrCreateInstallationId();

                            localBusy =
                                    false;

                            viewModel
                                    .exchangeFirebaseSession(
                                            firebaseIdToken,
                                            appCheckToken,
                                            installationId
                                    );

                            updateBusyState();
                        }
                );
    }


    private void showCodeStep() {

        editSmsCode.setVisibility(
                View.VISIBLE
        );

        buttonVerifyCode.setVisibility(
                View.VISIBLE
        );

        buttonResendCode.setVisibility(
                View.VISIBLE
        );
    }


    private void clearErrors() {

        editPhone.setError(
                null
        );

        editSmsCode.setError(
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

        localBusy =
                false;

        String safeMessage =
                clean(
                        message
                );

        if (safeMessage.isEmpty()) {

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

        updateBusyState();
    }


    private void updateBusyState() {

        boolean viewModelBusy =
                viewModel != null
                        &&
                        Boolean.TRUE.equals(
                                viewModel
                                        .getLoading()
                                        .getValue()
                        );

        boolean busy =
                localBusy
                        ||
                        viewModelBusy;

        progressLogin.setVisibility(
                busy
                        ? View.VISIBLE
                        : View.GONE
        );

        editPhone.setEnabled(
                !busy
        );

        editSmsCode.setEnabled(
                !busy
        );

        buttonSendCode.setEnabled(
                !busy
        );

        buttonVerifyCode.setEnabled(
                !busy
        );

        buttonResendCode.setEnabled(
                !busy
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


    private String clean(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
