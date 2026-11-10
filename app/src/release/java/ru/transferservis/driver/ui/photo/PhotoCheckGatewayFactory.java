package ru.transferservis.driver.ui.photo;

import ru.transferservis.driver.data.repository.DriverV1Repository;
import ru.transferservis.driver.data.repository.PhotoCheckGateway;

final class PhotoCheckGatewayFactory {
    private PhotoCheckGatewayFactory() {}
    static PhotoCheckGateway create() {
        return new DriverV1Repository();
    }
}
