package ru.transferservis.driver.data.model;

import java.util.List;

public final class DriverPhotoChecksResponse {

    private boolean ok;

    private List<DriverPhotoCheckDto> photoChecks;

    public boolean isOk() {
        return ok;
    }

    public List<DriverPhotoCheckDto> getPhotoChecks() {
        return photoChecks;
    }
}
