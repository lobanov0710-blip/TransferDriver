package ru.transferservis.driver.data.model;

public final class SupportCaseCreateRequest {

    private final String category;
    private final String orderId;

    public SupportCaseCreateRequest(
            String category,
            String orderId
    ) {
        this.category =
                category;

        this.orderId =
                orderId;
    }

    public String getCategory() {
        return category;
    }

    public String getOrderId() {
        return orderId;
    }
}
