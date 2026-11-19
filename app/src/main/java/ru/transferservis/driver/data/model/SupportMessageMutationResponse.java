package ru.transferservis.driver.data.model;

public final class SupportMessageMutationResponse {

    private boolean ok;

    private SupportMessageMutationDto message;

    private Long supportCaseRevision;

    public boolean isOk() {
        return ok;
    }

    public SupportMessageMutationDto getMessage() {
        return message;
    }

    public Long getSupportCaseRevision() {
        return supportCaseRevision;
    }
}
