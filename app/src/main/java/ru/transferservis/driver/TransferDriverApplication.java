package ru.transferservis.driver;

import android.app.Application;

import ru.transferservis.driver.data.api.ApiClient;

public final class TransferDriverApplication extends Application {

    @Override
    public void onCreate() {

        super.onCreate();

        ApiClient.initialize(
                this
        );
    }
}