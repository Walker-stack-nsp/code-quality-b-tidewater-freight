package com.tidewater.freight;

import com.tidewater.freight.model.PerKilogramRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PerKilogramRateTest {

    @Test
    void costIsRatePerKgTimesWeightPlusHandlingFee() {
        var regional = new PerKilogramRate(new BigDecimal("8.00"), new BigDecimal("10.00"));

        assertEquals(new BigDecimal("50.00"), regional.baseCost(new BigDecimal("5")));
    }

    @Test
    void zoneWithNoHandlingFeeCostsRateTimesWeight() {
        var domestic = new PerKilogramRate(new BigDecimal("5.00"), BigDecimal.ZERO);

        assertEquals(new BigDecimal("50.00"), domestic.baseCost(new BigDecimal("10")));
    }

    @Test
    void fractionalWeightIsPricedExactly() {
        var regional = new PerKilogramRate(new BigDecimal("8.00"), new BigDecimal("10.00"));

        assertEquals(new BigDecimal("28.400"), regional.baseCost(new BigDecimal("2.3")));
    }

    @Test
    void zeroRatePerKgGivesAFlatFeeZone() {
        var flatFee = new PerKilogramRate(BigDecimal.ZERO, new BigDecimal("30.00"));

        assertEquals(new BigDecimal("30.00"), flatFee.baseCost(new BigDecimal("7")));
    }

    @Test
    void negativeRatePerKgIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new PerKilogramRate(new BigDecimal("-0.01"), BigDecimal.ZERO));
    }

    @Test
    void negativeHandlingFeeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new PerKilogramRate(BigDecimal.ONE, new BigDecimal("-0.01")));
    }

    @Test
    void missingRatePerKgIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new PerKilogramRate(null, BigDecimal.ZERO));
    }

    @Test
    void missingHandlingFeeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new PerKilogramRate(BigDecimal.ONE, null));
    }
}
