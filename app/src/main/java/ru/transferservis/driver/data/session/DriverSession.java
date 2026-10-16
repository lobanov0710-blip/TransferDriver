package ru.transferservis.driver.data.session;

public final class DriverSession {

    private static final long ACCESS_SAFETY_WINDOW_MS =
            30_000L;

    private final String staffId;
    private final String displayName;
    private final String role;

    private final String accessToken;
    private final long accessExpiresAt;

    private final String refreshToken;
    private final long refreshExpiresAt;

    public DriverSession(
            String staffId,
            String displayName,
            String role,
            String accessToken,
            long accessExpiresAt,
            String refreshToken,
            long refreshExpiresAt
    ) {

        this.staffId =
                staffId;

        this.displayName =
                displayName;

        this.role =
                role;

        this.accessToken =
                accessToken;

        this.accessExpiresAt =
                accessExpiresAt;

        this.refreshToken =
                refreshToken;

        this.refreshExpiresAt =
                refreshExpiresAt;
    }

    public String getStaffId() {

        return staffId;
    }

    public String getDisplayName() {

        return displayName;
    }

    public String getRole() {

        return role;
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

    public boolean isDriver() {

        return role != null
                &&
                "driver".equalsIgnoreCase(
                        role.trim()
                );
    }

    public boolean hasUsableRefreshToken() {

        return refreshToken != null
                &&
                !refreshToken
                        .trim()
                        .isEmpty()
                &&
                refreshExpiresAt
                        >
                        System.currentTimeMillis();
    }

    public boolean hasUsableAccessToken() {

        return accessToken != null
                &&
                !accessToken
                        .trim()
                        .isEmpty()
                &&
                accessExpiresAt
                        >
                        System.currentTimeMillis()
                                +
                                ACCESS_SAFETY_WINDOW_MS;
    }
}