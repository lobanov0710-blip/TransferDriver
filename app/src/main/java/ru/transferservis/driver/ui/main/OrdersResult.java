package ru.transferservis.driver.ui.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.transferservis.driver.data.model.StaffOrderDto;

public final class OrdersResult {

    public enum Type {
        SUCCESS,
        ERROR,
        AUTHENTICATION_REQUIRED
    }

    private final Type type;

    private final List<StaffOrderDto> orders;

    private final String message;

    private OrdersResult(
            Type type,
            List<StaffOrderDto> orders,
            String message
    ) {

        this.type =
                type;

        if (orders == null) {

            this.orders =
                    Collections.emptyList();

        }
        else {

            this.orders =
                    Collections.unmodifiableList(
                            new ArrayList<>(
                                    orders
                            )
                    );
        }

        this.message =
                message;
    }

    public static OrdersResult success(
            List<StaffOrderDto> orders
    ) {

        return new OrdersResult(
                Type.SUCCESS,
                orders,
                null
        );
    }

    public static OrdersResult error(
            String message
    ) {

        return new OrdersResult(
                Type.ERROR,
                null,
                message
        );
    }

    public static OrdersResult authenticationRequired() {

        return new OrdersResult(
                Type.AUTHENTICATION_REQUIRED,
                null,
                null
        );
    }

    public Type getType() {

        return type;
    }

    public List<StaffOrderDto> getOrders() {

        return orders;
    }

    public String getMessage() {

        return message;
    }
}