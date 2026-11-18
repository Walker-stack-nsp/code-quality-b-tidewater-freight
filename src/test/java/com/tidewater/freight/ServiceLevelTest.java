package com.tidewater.freight;

import com.tidewater.freight.enums.ServiceLevel;
import com.tidewater.freight.exceptions.UnknownServiceLevelException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceLevelTest {

    @Test
    void standardLeavesTheCostUnchanged() {
        assertEquals(new BigDecimal("100.0"), ServiceLevel.STANDARD.applyTo(new BigDecimal("100")));
    }

    @Test
    void expressAddsHalfTheCost() {
        assertEquals(new BigDecimal("150.0"), ServiceLevel.EXPRESS.applyTo(new BigDecimal("100")));
    }

    @Test
    void codeIsReadIgnoringCaseAndSpaces() {
        assertEquals(ServiceLevel.EXPRESS, ServiceLevel.fromCode(" express "));
    }

    @Test
    void unknownCodeIsRejectedWithTheCodeAndOriginalCause() {
        var refusal = assertThrows(UnknownServiceLevelException.class,
                () -> ServiceLevel.fromCode("OVERNIGHT"));

        assertTrue(refusal.getMessage().contains("OVERNIGHT"));
        assertInstanceOf(IllegalArgumentException.class, refusal.getCause());
    }

    @Test
    void blankCodeIsRejected() {
        assertThrows(UnknownServiceLevelException.class, () -> ServiceLevel.fromCode("  "));
    }

    @Test
    void missingCodeIsRejected() {
        assertThrows(UnknownServiceLevelException.class, () -> ServiceLevel.fromCode(null));
    }
}
