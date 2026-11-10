package ru.transferservis.driver.data.repository;

import java.io.File;
import ru.transferservis.driver.data.model.DriverPhotoCheckDto;

public interface PhotoCheckGateway {
    void getPhotoChecks(DriverV1Repository.PhotoChecksCallback callback);
    void uploadPhotoCheckImage(DriverPhotoCheckDto check, String kind, File file,
            DriverV1Repository.UploadCallback callback);
}
