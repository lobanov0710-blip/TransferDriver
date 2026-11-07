package ru.transferservis.driver.data.model;

import java.util.List;

public final class DriverPhotoCheckDto {

    private String id;
    private String vehicleId;
    private String kind;
    private String reason;

    private Long dueAt;
    private Long graceUntil;

    private String result;
    private Long revision;
    private String restrictionMode;

    private Integer uploadedKindCount;
    private List<String> uploadedKinds;
    private Integer requiredKindCount;

    private Long createdAt;
    private Long updatedAt;

    public String getId() {
        return id;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getKind() {
        return kind;
    }

    public String getReason() {
        return reason;
    }

    public Long getDueAt() {
        return dueAt;
    }

    public Long getGraceUntil() {
        return graceUntil;
    }

    public String getResult() {
        return result;
    }

    public Long getRevision() {
        return revision;
    }

    public String getRestrictionMode() {
        return restrictionMode;
    }

    public Integer getUploadedKindCount() {
        return uploadedKindCount;
    }

    public List<String> getUploadedKinds() {
        return uploadedKinds;
    }

    public Integer getRequiredKindCount() {
        return requiredKindCount;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }
}
