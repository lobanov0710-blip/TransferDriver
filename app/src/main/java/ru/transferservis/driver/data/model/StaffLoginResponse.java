package ru.transferservis.driver.data.model;

public class StaffLoginResponse {

    private boolean ok;

    private StaffDto staff;

    private String accessToken;
    private long accessExpiresAt;

    private String refreshToken;
    private long refreshExpiresAt;

    public boolean isOk() {
        return ok;
    }

    public StaffDto getStaff() {
        return staff;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public long getAccessExpiresAt() {
        return accessExpiresAt;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getRefreshExpiresAt() {
        return refreshExpiresAt;
    }
}