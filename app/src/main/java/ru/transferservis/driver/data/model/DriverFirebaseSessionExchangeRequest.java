package ru.transferservis.driver.data.model;

public final class DriverFirebaseSessionExchangeRequest {

    private final String firebaseIdToken;
    private final String installationId;

    public DriverFirebaseSessionExchangeRequest(
            String firebaseIdToken,
            String installationId
    ) {
        this.firebaseIdToken =
                firebaseIdToken;

        this.installationId =
                installationId;
    }

    public String getFirebaseIdToken() {
        return firebaseIdToken;
    }

    public String getInstallationId() {
        return installationId;
    }
}
