package ru.transferservis.driver.data.model;

public final class SupportCaseResponse {

    private boolean ok;

    private SupportCaseDto supportCase;

    public boolean isOk() {
        return ok;
    }

    public SupportCaseDto getSupportCase() {
        return supportCase;
    }
}
