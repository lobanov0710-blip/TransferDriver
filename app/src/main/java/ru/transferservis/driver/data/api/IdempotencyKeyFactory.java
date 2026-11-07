package ru.transferservis.driver.data.api;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class IdempotencyKeyFactory {

    private IdempotencyKeyFactory() {
    }

    public static String forTerminalDone(
            String orderId,
            long expectedRevision
    ) {

        if (
                orderId == null
                        ||
                        orderId.trim().isEmpty()
                        ||
                        expectedRevision < 1
        ) {

            throw new IllegalArgumentException(
                    "Invalid terminal mutation identity"
            );
        }

        String canonical =
                "driver.order.done"
                        + "
"
                        + orderId.trim()
                        + "
"
                        + expectedRevision;

        return "android-done-"
                + sha256Hex(
                        canonical
                );
    }

    private static String sha256Hex(
            String value
    ) {

        final MessageDigest digest;

        try {

            digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

        }
        catch (
                NoSuchAlgorithmException error
        ) {

            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    error
            );
        }

        byte[] bytes =
                digest.digest(
                        value.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        StringBuilder output =
                new StringBuilder(
                        bytes.length * 2
                );

        for (byte current : bytes) {

            output.append(
                    String.format(
                            "%02x",
                            current & 0xff
                    )
            );
        }

        return output.toString();
    }
}
