package ru.transferservis.driver.ui.support;

import ru.transferservis.driver.data.repository.DriverV1Repository;
import ru.transferservis.driver.data.repository.SupportCaseGateway;

/** Release always uses the authenticated production repository. */
final class SupportCaseGatewayFactory {

    private SupportCaseGatewayFactory() {
    }

    static SupportCaseGateway create() {
        return new DriverV1Repository();
    }
}
