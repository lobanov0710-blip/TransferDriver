package ru.transferservis.driver.notifications;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public final class TransferFirebaseMessagingService
        extends FirebaseMessagingService {

    @Override
    public void onNewToken(
            @NonNull String token
    ) {

        super.onNewToken(
                token
        );

        /*
         * FCM registration tokens can rotate
         * at any time. They are not stable
         * device identifiers.
         */

        PushTokenStore tokenStore =
                new PushTokenStore(
                        this
                );

        tokenStore.saveToken(
                token
        );
    }

    @Override
    public void onMessageReceived(
            @NonNull RemoteMessage message
    ) {

        super.onMessageReceived(
                message
        );

        /*
         * Real push payload processing is
         * intentionally deferred to
         * ARCH-11.15B3.
         *
         * Do not log the message payload
         * or FCM registration token here.
         */
    }
}
