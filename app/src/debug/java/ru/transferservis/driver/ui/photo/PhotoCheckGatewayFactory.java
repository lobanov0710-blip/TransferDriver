package ru.transferservis.driver.ui.photo;

import ru.transferservis.driver.data.repository.DriverV1Repository;
import ru.transferservis.driver.data.repository.PhotoCheckGateway;

final class PhotoCheckGatewayFactory {
    private static volatile PhotoCheckGateway testGateway;
    private PhotoCheckGatewayFactory() {}
    static PhotoCheckGateway create() {
        PhotoCheckGateway gateway = testGateway;
        return gateway == null ? new DriverV1Repository() : gateway;
    }
    static void setTestGateway(PhotoCheckGateway gateway) {
        testGateway = gateway;
    }
}
