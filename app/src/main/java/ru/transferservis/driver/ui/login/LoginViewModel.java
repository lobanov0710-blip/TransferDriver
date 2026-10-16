package ru.transferservis.driver.ui.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import ru.transferservis.driver.data.model.StaffDto;
import ru.transferservis.driver.data.model.StaffLoginResponse;
import ru.transferservis.driver.data.repository.StaffRepository;

public class LoginViewModel extends ViewModel {

    private final StaffRepository repository;

    private final MutableLiveData<LoginResult> loginResult =
            new MutableLiveData<>();

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);

    public LoginViewModel() {

        repository =
                new StaffRepository();
    }

    public LiveData<LoginResult> getLoginResult() {

        return loginResult;
    }

    public LiveData<Boolean> getLoading() {

        return loading;
    }

    public void login(
            String login,
            String password
    ) {

        if (Boolean.TRUE.equals(
                loading.getValue()
        )) {

            return;
        }

        if (
                login == null
                        ||
                        login.trim().isEmpty()
        ) {

            loginResult.setValue(
                    LoginResult.error(
                            "Введите логин"
                    )
            );

            return;
        }

        if (
                password == null
                        ||
                        password.isEmpty()
        ) {

            loginResult.setValue(
                    LoginResult.error(
                            "Введите пароль"
                    )
            );

            return;
        }

        loading.setValue(
                true
        );

        repository.login(
                login,
                password,

                new StaffRepository.LoginCallback() {

                    @Override
                    public void onSuccess(
                            StaffLoginResponse response
                    ) {

                        loading.postValue(
                                false
                        );

                        if (!isValidDriverResponse(
                                response
                        )) {

                            loginResult.postValue(
                                    LoginResult.error(
                                            "Доступ разрешён только водителям"
                                    )
                            );

                            return;
                        }

                        loginResult.postValue(
                                LoginResult.success(
                                        response
                                )
                        );
                    }

                    @Override
                    public void onError(
                            int httpStatus,
                            String message
                    ) {

                        loading.postValue(
                                false
                        );

                        loginResult.postValue(
                                LoginResult.error(
                                        message
                                )
                        );
                    }
                }
        );
    }

    private boolean isValidDriverResponse(
            StaffLoginResponse response
    ) {

        if (
                response == null
                        ||
                        !response.isOk()
        ) {

            return false;
        }

        StaffDto staff =
                response.getStaff();

        if (
                staff == null
                        ||
                        staff.getId() == null
                        ||
                        staff.getId().trim().isEmpty()
        ) {

            return false;
        }

        if (
                staff.getRole() == null
                        ||
                        !"driver".equalsIgnoreCase(
                                staff.getRole().trim()
                        )
        ) {

            return false;
        }

        if (
                response.getAccessToken() == null
                        ||
                        response
                                .getAccessToken()
                                .trim()
                                .isEmpty()
        ) {

            return false;
        }

        if (
                response.getRefreshToken() == null
                        ||
                        response
                                .getRefreshToken()
                                .trim()
                                .isEmpty()
        ) {

            return false;
        }

        if (
                response.getAccessExpiresAt()
                        <= 0
                        ||
                        response.getRefreshExpiresAt()
                                <= 0
        ) {

            return false;
        }

        return true;
    }
}