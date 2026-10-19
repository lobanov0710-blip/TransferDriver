package ru.transferservis.driver;

import android.app.Application;

import com.google.firebase.messaging.FirebaseMessaging;

import ru.transferservis.driver.data.api.ApiClient;
import ru.transferservis.driver.notifications.DriverNotificationManager;
import ru.transferservis.driver.notifications.PushDeviceRegistrar;
import ru.transferservis.driver.notifications.PushTokenStore;

public final class TransferDriverApplication
        extends Application {

    @Override
    public void onCreate() {

        super.onCreate();

        ApiClient.initialize(
                this
        );

        DriverNotificationManager notificationManager =
                new DriverNotificationManager(
                        this
                );

        notificationManager
                .ensureNotificationChannel();

        initializePushToken();
    }

    private void initializePushToken() {

        PushTokenStore tokenStore =
                new PushTokenStore(
                        this
                );

        PushDeviceRegistrar pushRegistrar =
                new PushDeviceRegistrar(
                        this
                );

        FirebaseMessaging
                .getInstance()
                .getToken()
                .addOnSuccessListener(
                        token -> {

                            if (
                                    token == null
                                            ||
                                            token
                                                    .trim()
                                                    .isEmpty()
                            ) {

                                return;
                            }

                            /*
                             * Token is intentionally
                             * never written to logs.
                             */
                            tokenStore.saveToken(
                                    token
                            );

                            pushRegistrar
                                    .registerIfNeeded();
                        }
                );

        /*
         * Failure to obtain a token must not
         * crash application startup.
         *
         * Firebase may provide a token later.
         */
    }
}
