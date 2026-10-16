package ru.transferservis.driver.data.model;

public final class StaffOrderDto {

    private String id;

    private String route;
    private String from;
    private String to;

    private String date;
    private String tariff;

    private Double distance;
    private Integer duration;
    private Long price;

    private String status;
    private String driverId;

    private Long createdAt;
    private Long updatedAt;

    private String name;
    private String phone;
    private String comment;

    public String getId() {
        return id;
    }

    public String getRoute() {
        return route;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getDate() {
        return date;
    }

    public String getTariff() {
        return tariff;
    }

    public Double getDistance() {
        return distance;
    }

    public Integer getDuration() {
        return duration;
    }

    public Long getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }

    public String getDriverId() {
        return driverId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getComment() {
        return comment;
    }
}