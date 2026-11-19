package ru.transferservis.driver.data.model;

public final class SupportMessageDto {

    private String id;
    private String senderKind;
    private String senderAccountId;
    private String body;
    private Long createdAt;
    private Long expiresAt;

    public String getId() {
        return id;
    }

    public String getSenderKind() {
        return senderKind;
    }

    public String getSenderAccountId() {
        return senderAccountId;
    }

    public String getBody() {
        return body;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }
}
