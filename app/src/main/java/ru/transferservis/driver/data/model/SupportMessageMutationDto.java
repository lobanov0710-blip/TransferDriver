package ru.transferservis.driver.data.model;

public final class SupportMessageMutationDto {

    private String id;
    private String supportCaseId;
    private String senderKind;
    private Long createdAt;
    private Long expiresAt;

    public String getId() {
        return id;
    }

    public String getSupportCaseId() {
        return supportCaseId;
    }

    public String getSenderKind() {
        return senderKind;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }
}
