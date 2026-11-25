package ru.transferservis.driver.security;

import org.junit.Test;
import java.util.UUID;
import static org.junit.Assert.*;

public class UuidValidatorTest {
    @Test public void acceptsCanonicalUuidIncludingUppercase() {
        assertTrue(UuidValidator.isCanonical("123e4567-e89b-12d3-a456-426614174000"));
        assertTrue(UuidValidator.isCanonical("123E4567-E89B-12D3-A456-426614174000"));
    }

    @Test public void rejectsParserAliasesAndMalformedPushIds() {
        String alias = "000000000-000-0000-0000-000000000000";
        assertEquals(36, alias.length());
        UUID.fromString(alias); // The standard parser accepts this alias.
        assertFalse(UuidValidator.isCanonical(alias));
        for (String input : new String[] {"", "1-1-1-1-1", "../staff/logout",
                " 123e4567-e89b-12d3-a456-426614174000",
                "123e4567-e89b-12d3-a456-42661417400g"}) {
            assertFalse(UuidValidator.isCanonical(input));
        }
        assertFalse(UuidValidator.isCanonical(null));
    }
}
