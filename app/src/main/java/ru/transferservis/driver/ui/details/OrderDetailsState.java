package ru.transferservis.driver.ui.details;

import ru.transferservis.driver.data.model.StaffOrderDto;

public final class OrderDetailsState {

    public enum Type {
        SUCCESS,
        ERROR,
        NOT_FOUND,
        AUTHENTICATION_REQUIRED
    }

    private final Type type;
    private final StaffOrderDto order;
    private final String message;

    private OrderDetailsState(
            Type type,
            StaffOrderDto order,
            String message
    ) {

        this.type = type;
        this.order = order;
        this.message = message;
    }

    public static OrderDetailsState success(
            StaffOrderDto order
    ) {

        return new OrderDetailsState(
                Type.SUCCESS,
                order,
                null
        );
    }

    public static OrderDetailsState error(
            String message
    ) {

        return new OrderDetailsState(
                Type.ERROR,
                null,
                message
        );
    }

    public static OrderDetailsState notFound() {

        return new OrderDetailsState(
                Type.NOT_FOUND,
                null,
                null
        );
    }

    public static OrderDetailsState authenticationRequired() {

        return new OrderDetailsState(
                Type.AUTHENTICATION_REQUIRED,
                null,
                null
        );
    }

    public Type getType() {

        return type;
    }

    public StaffOrderDto getOrder() {

        return order;
    }

    public String getMessage() {

        return message;
    }
}