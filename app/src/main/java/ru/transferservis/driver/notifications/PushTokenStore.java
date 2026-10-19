package ru.transferservis.driver.notifications;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public final class PushTokenStore {

    private static final String PREFS_NAME =
            "driver_push_v1";

    private static final String KEY_FCM_TOKEN =
            "fcm_token";

    private static final String KEY_INSTALLATION_ID =
            "installation_id";

    private static final String KEY_REGISTRATION_DIRTY =
            "registration_dirty";

    private static final String KEY_REGISTRATION_GENERATION =
            "registration_generation";

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

    public synchronized void saveToken(
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
                .putLong(
                        KEY_REGISTRATION_GENERATION,
                        nextGeneration()
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

    public synchronized String getOrCreateInstallationId() {

        String current =
                getInstallationId();

        if (!current.isEmpty()) {

            return current;
        }

        String generated =
                UUID
                        .randomUUID()
                        .toString()
                        .toLowerCase();

        preferences
                .edit()
                .putString(
                        KEY_INSTALLATION_ID,
                        generated
                )
                .apply();

        return generated;
    }

    public String getInstallationId() {

        String value =
                clean(
                        preferences.getString(
                                KEY_INSTALLATION_ID,
                                null
                        )
                );

        if (!isUuid(value)) {

            return "";
        }

        return value
                .toLowerCase();
    }

    public boolean needsRegistration() {

        return hasToken()
                &&
                preferences.getBoolean(
                        KEY_REGISTRATION_DIRTY,
                        true
                );
    }

    public long getRegistrationGeneration() {

        return preferences.getLong(
                KEY_REGISTRATION_GENERATION,
                0L
        );
    }

    public synchronized void markRegistered(
            String token,
            long generation
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
                        ||
                        generation
                                !=
                                getRegistrationGeneration()
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

    public synchronized void markNeedsRegistration() {

        if (!hasToken()) {
            return;
        }

        preferences
                .edit()
                .putBoolean(
                        KEY_REGISTRATION_DIRTY,
                        true
                )
                .putLong(
                        KEY_REGISTRATION_GENERATION,
                        nextGeneration()
                )
                .apply();
    }

    private long nextGeneration() {

        long current =
                getRegistrationGeneration();

        if (
                current
                        == Long.MAX_VALUE
        ) {

            return 1L;
        }

        return current + 1L;
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
