package ru.transferservis.driver.notifications;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.net.Uri;

import ru.transferservis.driver.R;
import ru.transferservis.driver.ui.details.OrderDetailsActivity;
import ru.transferservis.driver.ui.main.DriverMainActivity;

public final class DriverNotificationManager {

    public static final String CHANNEL_NEW_ORDERS =
            "driver_new_orders_v1";

    private final Context context;

    private final NotificationManager notificationManager;

    public DriverNotificationManager(
            Context context
    ) {

        this.context =
                context.getApplicationContext();

        notificationManager =
                (NotificationManager)
                        this.context
                                .getSystemService(
                                        Context.NOTIFICATION_SERVICE
                                );
    }

    public void ensureNotificationChannel() {

        if (
                Build.VERSION.SDK_INT
                        < Build.VERSION_CODES.O
        ) {

            return;
        }

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL_NEW_ORDERS,
                        context.getString(
                                R.string.notification_channel_orders
                        ),
                        NotificationManager.IMPORTANCE_HIGH
                );

        channel.setDescription(
                context.getString(
                        R.string.notification_channel_orders_description
                )
        );

        channel.enableVibration(
                true
        );

        notificationManager.createNotificationChannel(
                channel
        );
    }

    public void notifyAssignment(
            String orderId
    ) {

        String normalizedOrderId =
                clean(
                        orderId
                );

        if (normalizedOrderId.isEmpty()) {
            return;
        }

        if (!canPostNotifications()) {
            return;
        }

        ensureNotificationChannel();

        Intent openOrderIntent =
                OrderDetailsActivity.createIntent(
                        context,
                        normalizedOrderId
                );

        openOrderIntent.setData(new Uri.Builder().scheme("transfer-driver")
                .authority("assignment").appendPath(normalizedOrderId).build());

        Intent openMainIntent =
                new Intent(
                        context,
                        DriverMainActivity.class
                );

        openMainIntent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivities(
                        context,
                        0,
                        new Intent[] {
                                openMainIntent,
                                openOrderIntent
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT
                                |
                                PendingIntent.FLAG_IMMUTABLE
                );

        Notification notification =
                new Notification.Builder(
                        context,
                        CHANNEL_NEW_ORDERS
                )
                        .setSmallIcon(
                                R.drawable.ic_notification_driver
                        )
                        .setContentTitle(
                                context.getString(
                                        R.string.notification_assignment_title
                                )
                        )
                        .setContentText(
                                context.getString(
                                        R.string.notification_assignment_text
                                )
                        )
                        .setStyle(
                                new Notification.BigTextStyle()
                                        .bigText(
                                                context.getString(
                                                        R.string.notification_assignment_text
                                                )
                                        )
                        )
                        .setContentIntent(
                                pendingIntent
                        )
                        .setAutoCancel(
                                true
                        )
                        .setCategory(
                                Notification.CATEGORY_TRANSPORT
                        )
                        .setVisibility(
                                Notification.VISIBILITY_PRIVATE
                        )
                        .build();

        notificationManager.notify(
                normalizedOrderId,
                0,
                notification
        );
    }

    public void cancelOrder(String orderId) {
        notificationManager.cancel(clean(orderId), 0);
    }

    private boolean canPostNotifications() {

        if (
                Build.VERSION.SDK_INT
                        < Build.VERSION_CODES.TIRAMISU
        ) {

            return true;
        }

        return context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private String clean(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
