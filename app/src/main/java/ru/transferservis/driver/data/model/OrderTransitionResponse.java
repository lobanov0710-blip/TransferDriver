package ru.transferservis.driver.data.model;

public final class OrderTransitionResponse {

    private boolean ok;

    private OrderTransitionDto order;

    public boolean isOk() {

        return ok;
    }

    public OrderTransitionDto getOrder() {

        return order;
    }
}