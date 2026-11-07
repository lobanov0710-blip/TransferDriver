package ru.transferservis.driver.data.model;

public final class OrderStatusRequest {

    private final String status;

    private final long expectedRevision;

    public OrderStatusRequest(
            String status,
            long expectedRevision
    ) {

        this.status =
                status;

        this.expectedRevision =
                expectedRevision;
    }

    public String getStatus() {

        return status;
    }

    public long getExpectedRevision() {

        return expectedRevision;
    }
}
