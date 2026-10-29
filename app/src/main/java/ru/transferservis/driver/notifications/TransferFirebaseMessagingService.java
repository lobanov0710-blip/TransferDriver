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
         * Driver push uses data-only FCM
         * messages so delivery is handled
         * consistently in foreground and
         * background.
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
         * after logout. Do not surface or act
         * on it unless a refreshable driver
         * session still exists locally.
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

        DriverNotificationManager notificationManager =
                new DriverNotificationManager(
                        this
                );

        if (
                "new_order".equals(
                        type
                )
        ) {

            notificationManager
                    .notifyNewOrder(
                            orderId
                    );

            return;
        }

        if (
                !"order_status_changed".equals(
                        type
                )
        ) {

            return;
        }

        String status =
                clean(
                        data.get(
                                "status"
                        )
                );

        String action =
                clean(
                        data.get(
                                "action"
                        )
                );

        String updatedAt =
                clean(
                        data.get(
                                "updatedAt"
                        )
                );

        if (
                !isPositiveLong(
                        updatedAt
                )
        ) {

            return;
        }

        /*
         * invalidate:
         * remove a stale "new order"
         * notification without showing a new
         * notification to the driver.
         */
        if (
                "invalidate".equals(
                        action
                )
                        &&
                        (
                                "taken".equals(
                                        status
                                )
                                        ||
                                        "canceled".equals(
                                                status
                                        )
                        )
        ) {

            notificationManager
                    .cancelOrderNotification(
                            orderId
                    );

            return;
        }

        /*
         * notify + canceled:
         * this event is sent only to the
         * driver assigned to the order.
         */
        if (
                "notify".equals(
                        action
                )
                        &&
                        "canceled".equals(
                                status
                        )
        ) {

            notificationManager
                    .notifyOrderCanceled(
                            orderId
                    );
        }
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

    private boolean isPositiveLong(
            String value
    ) {

        if (
                value == null
                        ||
                        value.trim().isEmpty()
        ) {

            return false;
        }

        try {

            return Long.parseLong(
                    value
            ) > 0L;

        } catch (
                NumberFormatException error
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
