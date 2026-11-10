package ru.transferservis.driver.ui.support;

import ru.transferservis.driver.data.repository.DriverV1Repository;
import ru.transferservis.driver.data.repository.SupportCaseGateway;

/**
 * Debug-only injection point for Android instrumentation tests.
 * The release variant has no test override.
 */
final class SupportCaseGatewayFactory {

    private static volatile SupportCaseGateway testGateway;

    private SupportCaseGatewayFactory() {
    }

    static SupportCaseGateway create() {
        SupportCaseGateway gateway = testGateway;
        return gateway != null ? gateway : new DriverV1Repository();
    }

    static void setTestGateway(SupportCaseGateway gateway) {
        testGateway = gateway;
    }
}
