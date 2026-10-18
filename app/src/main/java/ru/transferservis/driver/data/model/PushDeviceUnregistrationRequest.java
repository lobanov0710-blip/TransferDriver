package ru.transferservis.driver.data.model;

public final class PushDeviceUnregistrationRequest {

    private final String installationId;

    public PushDeviceUnregistrationRequest(
            String installationId
    ) {

        this.installationId =
                installationId;
    }
}
