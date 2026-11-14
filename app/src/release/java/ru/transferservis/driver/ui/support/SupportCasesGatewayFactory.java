package ru.transferservis.driver.ui.support;

import ru.transferservis.driver.data.repository.DriverV1Repository;
import ru.transferservis.driver.data.repository.SupportCasesGateway;

/** Release always uses the authenticated repository, never the debug fake. */
final class SupportCasesGatewayFactory {

    private SupportCasesGatewayFactory() {
    }

    static SupportCasesGateway create() {
        return new DriverV1Repository();
    }
}
