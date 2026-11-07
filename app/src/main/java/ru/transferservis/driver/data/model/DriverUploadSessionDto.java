package ru.transferservis.driver.data.model;

public final class DriverUploadSessionDto {

    private String id;
    private String method;
    private String path;
    private String contentType;
    private Long contentLength;
    private Long expiresAt;

    public String getId() { return id; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public String getContentType() { return contentType; }
    public Long getContentLength() { return contentLength; }
    public Long getExpiresAt() { return expiresAt; }
}
