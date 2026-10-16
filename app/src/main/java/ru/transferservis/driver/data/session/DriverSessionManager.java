package ru.transferservis.driver.data.session;

import android.content.Context;

import ru.transferservis.driver.data.repository.StaffRepository;

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

    public DriverSessionManager(
            Context context
    ) {

        sessionStore =
                new SecureSessionStore(
                        context
                );

        repository =
                new StaffRepository();

        sessionRefresher =
                new StaffSessionRefresher(
                        sessionStore
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

            sessionStore.clear();

            callback
                    .onAuthenticationRequired();

            return;
        }

        if (
                current
                        .hasUsableAccessToken()
        ) {

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

            sessionStore.clear();

            callback.onLoggedOut();

            return;
        }

        repository.logout(
                current.getRefreshToken(),

                new StaffRepository.LogoutCallback() {

                    @Override
                    public void onSuccess() {

                        sessionStore.clear();

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