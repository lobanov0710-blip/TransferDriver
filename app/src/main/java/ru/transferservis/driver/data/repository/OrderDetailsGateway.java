package ru.transferservis.driver.data.repository;

/**
 * Assigned-only order details and terminal mutation operations.
 * Release uses the authenticated OrderRepository implementation.
 */
public interface OrderDetailsGateway {

    void getOrders(
            String status,
            int limit,
            OrderRepository.OrdersCallback callback
    );

    void changeOrderStatus(
            String orderId,
            String targetStatus,
            long expectedRevision,
            OrderRepository.TransitionCallback callback
    );
}
