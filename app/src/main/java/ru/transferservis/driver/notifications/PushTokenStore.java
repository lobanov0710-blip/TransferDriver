package ru.transferservis.driver.notifications;

import android.content.Context;
import android.content.SharedPreferences;

public final class PushTokenStore {

    private static final String PREFS_NAME =
            "driver_push_v1";

    private static final String KEY_FCM_TOKEN =
            "fcm_token";

    private static final String KEY_REGISTRATION_DIRTY =
            "registration_dirty";

    private final SharedPreferences preferences;

    public PushTokenStore(
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

    public void saveToken(
            String token
    ) {

        String normalized =
                clean(
                        token
                );

        if (normalized.isEmpty()) {
            return;
        }

        String current =
                getToken();

        if (
                normalized.equals(
                        current
                )
        ) {

            return;
        }

        preferences
                .edit()
                .putString(
                        KEY_FCM_TOKEN,
                        normalized
                )
                .putBoolean(
                        KEY_REGISTRATION_DIRTY,
                        true
                )
                .apply();
    }

    public String getToken() {

        return clean(
                preferences.getString(
                        KEY_FCM_TOKEN,
                        null
                )
        );
    }

    public boolean hasToken() {

        return !getToken()
                .isEmpty();
    }

    public boolean needsRegistration() {

        return hasToken()
                &&
                preferences.getBoolean(
                        KEY_REGISTRATION_DIRTY,
                        true
                );
    }

    public void markRegistered(
            String token
    ) {

        String normalized =
                clean(
                        token
                );

        String current =
                getToken();

        if (
                normalized.isEmpty()
                        ||
                        !normalized.equals(
                                current
                        )
        ) {

            return;
        }

        preferences
                .edit()
                .putBoolean(
                        KEY_REGISTRATION_DIRTY,
                        false
                )
                .apply();
    }

    public void markNeedsRegistration() {

        if (!hasToken()) {
            return;
        }

        preferences
                .edit()
                .putBoolean(
                        KEY_REGISTRATION_DIRTY,
                        true
                )
                .apply();
    }

    private String clean(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
