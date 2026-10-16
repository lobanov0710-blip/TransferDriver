package ru.transferservis.driver.data.model;

public final class StaffRefreshTokenRequest {

    private final String refreshToken;

    public StaffRefreshTokenRequest(
            String refreshToken
    ) {

        this.refreshToken =
                refreshToken;
    }

    public String getRefreshToken() {

        return refreshToken;
    }
}