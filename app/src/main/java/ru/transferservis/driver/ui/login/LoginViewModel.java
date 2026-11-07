package ru.transferservis.driver.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import ru.transferservis.driver.data.model.StaffLoginResponse;
import ru.transferservis.driver.data.repository.DriverFirebaseSessionRepository;

public final class LoginViewModel
        extends ViewModel {

    private final DriverFirebaseSessionRepository repository;

    private final MutableLiveData<LoginResult> loginResult =
            new MutableLiveData<>();

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(
                    false
            );

    public LoginViewModel() {

        repository =
                new DriverFirebaseSessionRepository();
    }

    public LiveData<LoginResult> getLoginResult() {

        return loginResult;
    }

    public LiveData<Boolean> getLoading() {

        return loading;
    }

    public void exchangeFirebaseSession(
            String firebaseIdToken,
            String appCheckToken,
            String installationId
    ) {

        if (
                Boolean.TRUE.equals(
                        loading.getValue()
                )
        ) {
            return;
        }

        loading.setValue(
                true
        );

        repository.exchange(
                firebaseIdToken,
                appCheckToken,
                installationId,
                new DriverFirebaseSessionRepository.SessionCallback() {

                    @Override
                    public void onSuccess(
                            StaffLoginResponse response
                    ) {

                        loading.postValue(
                                false
                        );

                        loginResult.postValue(
                                LoginResult.success(
                                        response
                                )
                        );
                    }

                    @Override
                    public void onIdentityVerificationRequired() {

                        loading.postValue(
                                false
                        );

                        loginResult.postValue(
                                LoginResult.error(
                                        "Не удалось подтвердить номер. Запросите новый код"
                                )
                        );
                    }

                    @Override
                    public void onRateLimited() {

                        loading.postValue(
                                false
                        );

                        loginResult.postValue(
                                LoginResult.error(
                                        "Слишком много попыток. Повторите позже"
                                )
                        );
                    }

                    @Override
                    public void onTemporaryError(
                            String message
                    ) {

                        loading.postValue(
                                false
                        );

                        loginResult.postValue(
                                LoginResult.error(
                                        safeMessage(
                                                message
                                        )
                                )
                        );
                    }
                }
        );
    }

    private String safeMessage(
            String value
    ) {

        if (
                value == null
                        ||
                        value.trim().isEmpty()
        ) {

            return "Не удалось подтвердить вход";
        }

        return value.trim();
    }
}
