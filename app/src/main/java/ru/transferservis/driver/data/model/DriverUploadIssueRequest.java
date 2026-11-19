package ru.transferservis.driver.data.model;

public final class DriverUploadIssueRequest {

    private final String resourceType;
    private final String kind;
    private final String vehicleId;
    private final String photoCheckId;
    private final String contentType;
    private final long contentLength;

    public DriverUploadIssueRequest(
            String resourceType,
            String kind,
            String vehicleId,
            String photoCheckId,
            String contentType,
            long contentLength
    ) {
        this.resourceType = resourceType;
        this.kind = kind;
        this.vehicleId = vehicleId;
        this.photoCheckId = photoCheckId;
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    public String getResourceType() { return resourceType; }
    public String getKind() { return kind; }
    public String getVehicleId() { return vehicleId; }
    public String getPhotoCheckId() { return photoCheckId; }
    public String getContentType() { return contentType; }
    public long getContentLength() { return contentLength; }
}
