package ru.transferservis.driver.data.model;

public final class DriverUploadIssueResponse {

    private boolean ok;
    private DriverUploadSessionDto upload;

    public boolean isOk() { return ok; }
    public DriverUploadSessionDto getUpload() { return upload; }
}
