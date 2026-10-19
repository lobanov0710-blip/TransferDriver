package ru.transferservis.driver.notifications;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.UUID;

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

        /*
         * ARCH-11.15B3 uses data-only FCM
         * messages so delivery is handled
         * here consistently in foreground
         * and background.
         *
         * Never log payloads or FCM tokens.
         */

        Map<String, String> data =
                message.getData();

        if (
                data == null
                        ||
                        data.isEmpty()
        ) {

            return;
        }

        String type =
                clean(
                        data.get(
                                "type"
                        )
                );

        if (!"new_order".equals(
                type
        )) {

            return;
        }

        String orderId =
                clean(
                        data.get(
                                "orderId"
                        )
                );

        if (!isUuid(
                orderId
        )) {

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

        new DriverNotificationManager(
                this
        )
                .notifyNewOrder(
                        orderId
                );
    }

    private boolean isUuid(
            String value
    ) {

        if (
                value == null
                        ||
                        value.length()
                                != 36
        ) {

            return false;
        }

        try {

            UUID.fromString(
                    value
            );

            return true;

        } catch (
                IllegalArgumentException error
        ) {

            return false;
        }
    }

    private String clean(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
