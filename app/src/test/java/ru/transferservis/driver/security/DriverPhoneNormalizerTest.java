package ru.transferservis.driver.security;

import org.junit.Test;

import static org.junit.Assert.*;

public final class DriverPhoneNormalizerTest {

    @Test
    public void normalizesRussianDriverPhoneForms() {

        assertEquals(
                "+79991234567",
                DriverPhoneNormalizer.normalize(
                        "8 (999) 123-45-67"
                )
        );

        assertEquals(
                "+79991234567",
                DriverPhoneNormalizer.normalize(
                        "7 999 123 45 67"
                )
        );

        assertEquals(
                "+79991234567",
                DriverPhoneNormalizer.normalize(
                        "9991234567"
                )
        );

        assertEquals(
                "+79991234567",
                DriverPhoneNormalizer.normalize(
                        "+7 (999) 123-45-67"
                )
        );
    }

    @Test
    public void preservesValidInternationalE164Input() {

        assertEquals(
                "+14155552671",
                DriverPhoneNormalizer.normalize(
                        "+1 415 555 2671"
                )
        );
    }

    @Test
    public void rejectsMalformedPhoneInput() {

        assertNull(
                DriverPhoneNormalizer.normalize(
                        null
                )
        );

        assertNull(
                DriverPhoneNormalizer.normalize(
                        ""
                )
        );

        assertNull(
                DriverPhoneNormalizer.normalize(
                        "phone"
                )
        );

        assertNull(
                DriverPhoneNormalizer.normalize(
                        "+0123456789"
                )
        );

        assertNull(
                DriverPhoneNormalizer.normalize(
                        "123"
                )
        );
    }
}
