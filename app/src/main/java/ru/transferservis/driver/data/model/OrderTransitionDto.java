package ru.transferservis.driver.data.model;

public final class OrderTransitionDto {

    private String id;

    private String status;

    private String driverId;

    private Long updatedAt;

    public String getId() {

        return id;
    }

    public String getStatus() {

        return status;
    }

    public String getDriverId() {

        return driverId;
    }

    public Long getUpdatedAt() {

        return updatedAt;
    }
}