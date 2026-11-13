package ru.transferservis.driver.data.model;

import java.util.List;

public final class SupportCasesResponse {

    private boolean ok;

    private List<SupportCaseDto> supportCases;

    public boolean isOk() {
        return ok;
    }

    public List<SupportCaseDto> getSupportCases() {
        return supportCases;
    }
}
