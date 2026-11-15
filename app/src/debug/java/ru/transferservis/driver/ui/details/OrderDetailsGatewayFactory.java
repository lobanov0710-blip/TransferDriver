package ru.transferservis.driver.ui.details;

import ru.transferservis.driver.data.repository.OrderDetailsGateway;
import ru.transferservis.driver.data.repository.OrderRepository;

/** Debug-only gateway override for isolated UI instrumentation. */
final class OrderDetailsGatewayFactory {

    private static volatile OrderDetailsGateway testGateway;

    private OrderDetailsGatewayFactory() {
    }

    static OrderDetailsGateway create() {
        OrderDetailsGateway gateway = testGateway;
        return gateway != null ? gateway : new OrderRepository();
    }

    static void setTestGateway(OrderDetailsGateway gateway) {
        testGateway = gateway;
    }
}
