package ru.transferservis.driver.data.model;

import java.util.List;

public final class OrdersResponse {

    private boolean ok;

    private int total;
    private int count;
    private int limit;

    private String status;

    private List<StaffOrderDto> orders;

    public boolean isOk() {
        return ok;
    }

    public int getTotal() {
        return total;
    }

    public int getCount() {
        return count;
    }

    public int getLimit() {
        return limit;
    }

    public String getStatus() {
        return status;
    }

    public List<StaffOrderDto> getOrders() {
        return orders;
    }

    public static OrdersResponse assignedOnly(
            List<StaffOrderDto> orders,
            int limit,
            String status
    ) {

        OrdersResponse response =
                new OrdersResponse();

        response.ok =
                true;

        response.orders =
                orders;

        response.count =
                orders == null
                        ? 0
                        : orders.size();

        response.total =
                response.count;

        response.limit =
                limit;

        response.status =
                status;

        return response;
    }
}