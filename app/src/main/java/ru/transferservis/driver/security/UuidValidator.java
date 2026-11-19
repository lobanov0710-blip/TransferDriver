package ru.transferservis.driver.security;

import java.util.UUID;

public final class UuidValidator {

    private UuidValidator() {
    }

    public static boolean isCanonical(String value) {
        if (value == null || value.length() != 36) {
            return false;
        }
        try {
            // UUID.fromString also accepts some shortened/noncanonical groups.
            return UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }
}
