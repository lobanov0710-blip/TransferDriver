package ru.transferservis.driver.data.session;

import android.content.Context;

import ru.transferservis.driver.data.repository.StaffRepository;
import ru.transferservis.driver.notifications.PushDeviceRegistrar;

public final class DriverSessionManager {

    public interface SessionCallback {

        void onReady(
                DriverSession session
        );

        void onAuthenticationRequired();

        void onTemporaryError(
                String message
        );
    }

    public interface LogoutCallback {

        void onLoggedOut();

        void onError(
                String message
        );
    }

    private final SecureSessionStore sessionStore;

    private final StaffRepository repository;

    private final StaffSessionRefresher sessionRefresher;

    private final PushDeviceRegistrar pushRegistrar;

    public DriverSessionManager(
            Context context
    ) {

        Context applicationContext =
                context
                        .getApplicationContext();

        sessionStore =
                new SecureSessionStore(
                        applicationContext
                );

        repository =
                new StaffRepository();

        sessionRefresher =
                new StaffSessionRefresher(
                        sessionStore
                );

        pushRegistrar =
                new PushDeviceRegistrar(
                        applicationContext
                );
    }

    public void ensureValidSession(
            SessionCallback callback
    ) {

        if (callback == null) {
            return;
        }

        DriverSession current =
                sessionStore.load();

        if (
                current == null
                        ||
                        !current.isDriver()
                        ||
                        !current
                                .hasUsableRefreshToken()
        ) {

            pushRegistrar
                    .markNeedsRegistration();

            sessionStore.clear();

            callback
                    .onAuthenticationRequired();

            return;
        }

        if (
                current
                        .hasUsableAccessToken()
        ) {

            pushRegistrar
                    .registerIfNeeded();

            callback.onReady(
                    current
            );

            return;
        }

        sessionRefresher
                .refreshExpiredSessionAsync(
                        result -> {

                            switch (
                                    result.getType()
                            ) {

                                case READY:

                                    pushRegistrar
                                            .registerIfNeeded();

                                    callback.onReady(
                                            result.getSession()
                                    );

                                    break;

                                case TEMPORARY_FAILURE:

                                    callback.onTemporaryError(
                                            "Не удалось обновить сессию"
                                    );

                                    break;

                                case AUTHENTICATION_REQUIRED:

                                    pushRegistrar
                                            .markNeedsRegistration();

                                    callback
                                            .onAuthenticationRequired();

                                    break;
                            }
                        }
                );
    }

    public void logout(
            LogoutCallback callback
    ) {

        if (callback == null) {
            return;
        }

        DriverSession current =
                sessionStore.load();

        if (current == null) {

            pushRegistrar
                    .markNeedsRegistration();

            sessionStore.clear();

            callback.onLoggedOut();

            return;
        }

        /*
         * DELETE /staff/push-device requires
         * a valid staff access context, so
         * perform it before staff/logout.
         *
         * Unregister is best effort and must
         * never prevent the actual logout.
         */
        pushRegistrar.unregisterBeforeLogout(
                () ->
                        logoutStaffSession(
                                current,
                                callback
                        )
        );
    }

    private void logoutStaffSession(
            DriverSession current,
            LogoutCallback callback
    ) {

        repository.logout(
                current.getRefreshToken(),

                new StaffRepository.LogoutCallback() {

                    @Override
                    public void onSuccess() {

                        sessionStore.clear();

                        /*
                         * A later login on this
                         * installation must bind
                         * the current FCM token to
                         * the new session family.
                         */
                        pushRegistrar
                                .markNeedsRegistration();

                        callback.onLoggedOut();
                    }

                    @Override
                    public void onError(
                            int httpStatus,
                            String message
                    ) {

                        callback.onError(
                                safeMessage(
                                        message,
                                        "Не удалось завершить сессию"
                                )
                        );
                    }
                }
        );
    }

    private String safeMessage(
            String message,
            String fallback
    ) {

        if (
                message == null
                        ||
                        message
                                .trim()
                                .isEmpty()
        ) {

            return fallback;
        }

        return message.trim();
    }
}
