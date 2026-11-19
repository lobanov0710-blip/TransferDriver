package ru.transferservis.driver.data.model;

import java.util.List;

public final class SupportCaseDto {

    private String id;
    private String driverId;
    private String orderId;
    private String category;
    private String status;
    private Long revision;
    private Long createdAt;
    private Long updatedAt;
    private String resolvedBy;

    private List<SupportMessageDto> messages;

    public String getId() {
        return id;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCategory() {
        return category;
    }

    public String getStatus() {
        return status;
    }

    public Long getRevision() {
        return revision;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public List<SupportMessageDto> getMessages() {
        return messages;
    }
}
