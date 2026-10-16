package ru.transferservis.driver.ui.main;

public final class OrderActionResult {

    public enum Type {
        SUCCESS,
        CONFLICT,
        ERROR,
        AUTHENTICATION_REQUIRED
    }

    private final Type type;

    private final String orderId;
    private final String targetStatus;
    private final String message;

    private OrderActionResult(
            Type type,
            String orderId,
            String targetStatus,
            String message
    ) {

        this.type =
                type;

        this.orderId =
                orderId;

        this.targetStatus =
                targetStatus;

        this.message =
                message;
    }

    public static OrderActionResult success(
            String orderId,
            String targetStatus
    ) {

        return new OrderActionResult(
                Type.SUCCESS,
                orderId,
                targetStatus,
                null
        );
    }

    public static OrderActionResult conflict(
            String orderId,
            String targetStatus,
            String message
    ) {

        return new OrderActionResult(
                Type.CONFLICT,
                orderId,
                targetStatus,
                message
        );
    }

    public static OrderActionResult error(
            String orderId,
            String targetStatus,
            String message
    ) {

        return new OrderActionResult(
                Type.ERROR,
                orderId,
                targetStatus,
                message
        );
    }

    public static OrderActionResult authenticationRequired(
            String orderId,
            String targetStatus
    ) {

        return new OrderActionResult(
                Type.AUTHENTICATION_REQUIRED,
                orderId,
                targetStatus,
                null
        );
    }

    public Type getType() {

        return type;
    }

    public String getOrderId() {

        return orderId;
    }

    public String getTargetStatus() {

        return targetStatus;
    }

    public String getMessage() {

        return message;
    }
}