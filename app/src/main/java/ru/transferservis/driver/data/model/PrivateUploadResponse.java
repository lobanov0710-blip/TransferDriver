package ru.transferservis.driver.data.model;

public final class PrivateUploadResponse {

    private boolean ok;
    private PrivateResourceDto resource;

    public boolean isOk() { return ok; }
    public PrivateResourceDto getResource() { return resource; }
}
