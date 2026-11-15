package ru.transferservis.driver.ui.details;

import ru.transferservis.driver.data.repository.OrderDetailsGateway;
import ru.transferservis.driver.data.repository.OrderRepository;

/** No test override exists in release builds. */
final class OrderDetailsGatewayFactory {

    private OrderDetailsGatewayFactory() {
    }

    static OrderDetailsGateway create() {
        return new OrderRepository();
    }
}
