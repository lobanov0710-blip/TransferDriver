package ru.transferservis.driver.data.model;

public final class DriverAssignmentDto {

    private String assignmentId;
    private Long assignmentRevision;
    private Long assignedAt;

    private String phase;
    private Long phaseUpdatedAt;

    private String vehicleId;

    private StaffOrderDto order;

    public String getAssignmentId() {

        return assignmentId;
    }

    public Long getAssignmentRevision() {

        return assignmentRevision;
    }

    public Long getAssignedAt() {

        return assignedAt;
    }

    public String getPhase() {

        return phase;
    }

    public Long getPhaseUpdatedAt() {

        return phaseUpdatedAt;
    }

    public String getVehicleId() {

        return vehicleId;
    }

    public StaffOrderDto getOrder() {

        return order;
    }
}
