package ru.transferservis.driver.notifications;

import android.content.Context;

import java.util.concurrent.atomic.AtomicBoolean;

import ru.transferservis.driver.data.repository.PushDeviceRepository;
import ru.transferservis.driver.data.session.DriverSession;
import ru.transferservis.driver.data.session.SecureSessionStore;

public final class PushDeviceRegistrar {

    public interface CompletionCallback {

        void onComplete();
    }

    private static final AtomicBoolean REGISTRATION_IN_FLIGHT =
            new AtomicBoolean(
                    false
            );

    private final PushTokenStore tokenStore;

    private final SecureSessionStore sessionStore;

    private final PushDeviceRepository repository;

    public PushDeviceRegistrar(
            Context context
    ) {

        Context applicationContext =
                context
                        .getApplicationContext();

        tokenStore =
                new PushTokenStore(
                        applicationContext
                );

        sessionStore =
                new SecureSessionStore(
                        applicationContext
                );

        repository =
                new PushDeviceRepository();
    }

    public void registerIfNeeded() {

        if (
                !tokenStore
                        .needsRegistration()
        ) {

            return;
        }

        DriverSession session =
                sessionStore.load();

        if (
                session == null
                        ||
                        !session.isDriver()
                        ||
                        !session
                                .hasUsableRefreshToken()
        ) {

            return;
        }

        String staffId =
                clean(
                        session.getStaffId()
                );

        String token =
                tokenStore.getToken();

        if (
                staffId.isEmpty()
                        ||
                        token.isEmpty()
        ) {

            return;
        }

        String installationId =
                tokenStore
                        .getOrCreateInstallationId();

        long generation =
                tokenStore
                        .getRegistrationGeneration();

        if (
                !REGISTRATION_IN_FLIGHT
                        .compareAndSet(
                                false,
                                true
                        )
        ) {

            return;
        }

        repository.register(
                installationId,
                token,

                new PushDeviceRepository.Callback() {

                    @Override
                    public void onSuccess() {

                        REGISTRATION_IN_FLIGHT.set(
                                false
                        );

                        DriverSession current =
                                sessionStore.load();

                        if (
                                current != null
                                        &&
                                        current.isDriver()
                                        &&
                                        staffId.equals(
                                                clean(
                                                        current.getStaffId()
                                                )
                                        )
                        ) {

                            tokenStore.markRegistered(
                                    token,
                                    generation
                            );
                        }

                        /*
                         * If the token rotated while
                         * this request was in flight,
                         * markRegistered() deliberately
                         * leaves the store dirty.
                         *
                         * Start the queued registration
                         * immediately after this request
                         * has released the process-wide
                         * in-flight guard.
                         */
                        if (
                                tokenStore
                                        .needsRegistration()
                        ) {

                            registerIfNeeded();
                        }
                    }

                    @Override
                    public void onError(
                            int httpStatus
                    ) {

                        REGISTRATION_IN_FLIGHT.set(
                                false
                        );

                        /*
                         * Keep registration_dirty=true.
                         *
                         * A later session/start/token
                         * trigger will retry. Push setup
                         * must never crash or block the
                         * driver workflow.
                         */
                    }
                }
        );
    }

    public void unregisterBeforeLogout(
            CompletionCallback callback
    ) {

        if (callback == null) {
            return;
        }

        /*
         * Invalidate any registration request
         * which may already be in flight.
         */
        tokenStore.markNeedsRegistration();

        String installationId =
                tokenStore
                        .getInstallationId();

        DriverSession session =
                sessionStore.load();

        if (
                installationId.isEmpty()
                        ||
                        session == null
                        ||
                        !session.isDriver()
                        ||
                        !session
                                .hasUsableRefreshToken()
        ) {

            callback.onComplete();

            return;
        }

        repository.unregister(
                installationId,

                new PushDeviceRepository.Callback() {

                    @Override
                    public void onSuccess() {

                        callback.onComplete();
                    }

                    @Override
                    public void onError(
                            int httpStatus
                    ) {

                        /*
                         * Unregister is best effort.
                         *
                         * The backend send path must
                         * also validate authoritative
                         * session-family state, so an
                         * unreachable unregister call
                         * must not prevent staff logout.
                         */
                        callback.onComplete();
                    }
                }
        );
    }

    public void markNeedsRegistration() {

        tokenStore.markNeedsRegistration();
    }

    private String clean(
            String value
    ) {

        return value == null
                ? ""
                : value.trim();
    }
}
