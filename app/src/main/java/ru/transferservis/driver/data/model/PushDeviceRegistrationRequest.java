package ru.transferservis.driver.data.model;

public final class PushDeviceRegistrationRequest {

    private final String installationId;
    private final String platform;
    private final String token;

    public PushDeviceRegistrationRequest(
            String installationId,
            String platform,
            String token
    ) {

        this.installationId =
                installationId;

        this.platform =
                platform;

        this.token =
                token;
    }
}
