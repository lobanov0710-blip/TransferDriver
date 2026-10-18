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

import java.util.List;

import ru.transferservis.driver.R;
import ru.transferservis.driver.data.model.StaffOrderDto;
import ru.transferservis.driver.ui.main.DriverMainActivity;

public final class DriverNotificationManager {

    public static final String CHANNEL_NEW_ORDERS =
            "driver_new_orders_v1";

    private static final int NOTIFICATION_NEW_ORDERS =
            11015;

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

    public void notifyNewOrders(
            List<StaffOrderDto> orders
    ) {

        if (
                orders == null
                        ||
                        orders.isEmpty()
        ) {

            return;
        }

        if (!canPostNotifications()) {

            return;
        }

        ensureNotificationChannel();

        Intent openAppIntent =
                new Intent(
                        context,
                        DriverMainActivity.class
                );

        openAppIntent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        context,
                        NOTIFICATION_NEW_ORDERS,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                |
                                PendingIntent.FLAG_IMMUTABLE
                );

        String title;
        String text;

        if (
                orders.size()
                        == 1
        ) {

            StaffOrderDto order =
                    orders.get(
                            0
                    );

            title =
                    context.getString(
                            R.string.notification_new_order_title
                    );

            text =
                    buildRoute(
                            order
                    );

        }
        else {

            title =
                    context.getString(
                            R.string.notification_new_orders_title,
                            orders.size()
                    );

            text =
                    context.getString(
                            R.string.notification_new_orders_text
                    );
        }

        Notification.Builder builder =
                new Notification.Builder(
                        context,
                        CHANNEL_NEW_ORDERS
                )
                        .setSmallIcon(
                                R.drawable.ic_notification_driver
                        )
                        .setContentTitle(
                                title
                        )
                        .setContentText(
                                text
                        )
                        .setStyle(
                                new Notification.BigTextStyle()
                                        .bigText(
                                                text
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
                        .setNumber(
                                orders.size()
                        );

        notificationManager.notify(
                NOTIFICATION_NEW_ORDERS,
                builder.build()
        );
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

    private String buildRoute(
            StaffOrderDto order
    ) {

        if (order == null) {

            return context.getString(
                    R.string.order_route_unknown
            );
        }

        String route =
                clean(
                        order.getRoute()
                );

        if (!route.isEmpty()) {

            return route;
        }

        String from =
                clean(
                        order.getFrom()
                );

        String to =
                clean(
                        order.getTo()
                );

        if (
                !from.isEmpty()
                        &&
                        !to.isEmpty()
        ) {

            return from
                    + " → "
                    + to;
        }

        if (!from.isEmpty()) {

            return from;
        }

        if (!to.isEmpty()) {

            return to;
        }

        return context.getString(
                R.string.order_route_unknown
        );
    }

    private String clean(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
