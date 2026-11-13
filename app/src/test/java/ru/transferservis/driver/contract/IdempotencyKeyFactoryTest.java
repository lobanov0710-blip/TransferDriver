package ru.transferservis.driver.contract;

import org.junit.Test;

import ru.transferservis.driver.data.api.IdempotencyKeyFactory;

import static org.junit.Assert.*;

public final class IdempotencyKeyFactoryTest {

    @Test
    public void sameTerminalMutationGetsSameKey() {

        String first =
                IdempotencyKeyFactory
                        .forTerminalDone(
                                "order_12345678",
                                7L
                        );

        String second =
                IdempotencyKeyFactory
                        .forTerminalDone(
                                "order_12345678",
                                7L
                        );

        assertEquals(
                first,
                second
        );

        assertTrue(
                first.matches(
                        "^android-done-[0-9a-f]{64}$"
                )
        );
    }


    @Test
    public void changedRevisionGetsDifferentKey() {

        assertNotEquals(
                IdempotencyKeyFactory
                        .forTerminalDone(
                                "order_12345678",
                                7L
                        ),
                IdempotencyKeyFactory
                        .forTerminalDone(
                                "order_12345678",
                                8L
                        )
        );
    }


    @Test(
            expected =
                    IllegalArgumentException.class
    )
    public void invalidRevisionIsRejected() {

        IdempotencyKeyFactory
                .forTerminalDone(
                        "order_12345678",
                        0L
                );
    }
}
