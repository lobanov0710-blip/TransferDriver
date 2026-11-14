package ru.transferservis.driver.ui.support;

import ru.transferservis.driver.data.repository.DriverV1Repository;
import ru.transferservis.driver.data.repository.SupportCasesGateway;

/** Debug-only test injection; the release variant has no override. */
final class SupportCasesGatewayFactory {

    private static volatile SupportCasesGateway testGateway;

    private SupportCasesGatewayFactory() {
    }

    static SupportCasesGateway create() {
        SupportCasesGateway gateway = testGateway;
        return gateway != null ? gateway : new DriverV1Repository();
    }

    static void setTestGateway(SupportCasesGateway gateway) {
        testGateway = gateway;
    }
}
