package ru.transferservis.driver.data.model;

public final class OrderStatusRequest {

    private final String status;

    public OrderStatusRequest(
            String status
    ) {

        this.status =
                status;
    }

    public String getStatus() {

        return status;
    }
}