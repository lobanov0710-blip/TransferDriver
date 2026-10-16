package ru.transferservis.driver.ui.login;

import ru.transferservis.driver.data.model.StaffLoginResponse;

public class LoginResult {

    private final boolean success;
    private final String message;
    private final StaffLoginResponse response;

    private LoginResult(
            boolean success,
            String message,
            StaffLoginResponse response
    ) {

        this.success = success;
        this.message = message;
        this.response = response;
    }

    public static LoginResult success(
            StaffLoginResponse response
    ) {

        return new LoginResult(
                true,
                null,
                response
        );
    }

    public static LoginResult error(
            String message
    ) {

        return new LoginResult(
                false,
                message,
                null
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public StaffLoginResponse getResponse() {
        return response;
    }
}