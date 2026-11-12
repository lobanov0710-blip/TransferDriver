package ru.transferservis.driver.notifications;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import ru.transferservis.driver.data.session.SecureSessionStore;

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

        /*
         * If an authenticated staff session
         * exists, update the backend now.
         *
         * Without a session this is a no-op;
         * login/session restoration retries it.
         */
        new PushDeviceRegistrar(
                this
        )
                .registerIfNeeded();
    }

    @Override
    public void onMessageReceived(
            @NonNull RemoteMessage message
    ) {

        super.onMessageReceived(
                message
        );

        AssignmentPushEvent event = AssignmentPushEvent.parse(message.getData());
        if (event == null) return;
        DriverNotificationManager notifications = new DriverNotificationManager(this);
        if (event.revoked) {
            notifications.cancelOrder(event.orderId);
            return;
        }

        /*
         * A delayed FCM message may arrive
         * after logout. Do not surface it
         * unless a refreshable driver session
         * still exists locally.
         */
        SecureSessionStore sessionStore =
                new SecureSessionStore(
                        this
                );

        if (
                !sessionStore
                        .hasRefreshableDriverSession()
        ) {

            return;
        }

        notifications.notifyAssignment(event.orderId);
    }
}
