package ru.transferservis.driver.notifications;

import android.content.Context;
import android.content.SharedPreferences;

public final class OrderPushVersionStore {

    private static final String PREFS_NAME =
            "driver_order_push_versions_v1";

    private final SharedPreferences preferences;

    public OrderPushVersionStore(
            Context context
    ) {

        preferences =
                context
                        .getApplicationContext()
                        .getSharedPreferences(
                                PREFS_NAME,
                                Context.MODE_PRIVATE
                        );
    }

    public synchronized boolean acceptIfNewer(
            String orderId,
            long updatedAt
    ) {

        String key =
                clean(
                        orderId
                );

        if (
                key.isEmpty()
                        ||
                        updatedAt <= 0L
        ) {

            return false;
        }

        long current =
                preferences.getLong(
                        key,
                        0L
                );

        if (
                updatedAt <= current
        ) {

            return false;
        }

        preferences
                .edit()
                .putLong(
                        key,
                        updatedAt
                )
                .apply();

        return true;
    }

    private String clean(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
