package com.tidewater.freight;

import com.tidewater.freight.model.Zone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ZoneTest {

    @Test
    void codesAreTrimmedAndCaseInsensitive() {
        assertEquals(new Zone("DOMESTIC"), new Zone("  domestic "));
    }

    @Test
    void printsAsItsCode() {
        assertEquals("REGIONAL", new Zone("regional").toString());
    }

    @Test
    void blankCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Zone("   "));
    }

    @Test
    void missingCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Zone(null));
    }
}
