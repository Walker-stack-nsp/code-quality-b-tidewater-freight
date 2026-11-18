package com.tidewater.freight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShippingCalculatorTest {

    private static final double EXACT = 1e-9;

    private final ShippingCalculator calculator = new ShippingCalculator();

    @Test
    void domesticStandardCostsRatePerKgTimesWeight() {
        assertEquals(50.0, calculator.calculateCost("DOMESTIC", 10, "STANDARD"), EXACT);
    }

    @Test
    void regionalAddsItsHandlingFee() {
        assertEquals(50.0, calculator.calculateCost("REGIONAL", 5, "STANDARD"), EXACT);
    }

    @Test
    void internationalAddsItsCustomsHandlingFee() {
        assertEquals(175.0, calculator.calculateCost("INTERNATIONAL", 10, "STANDARD"), EXACT);
    }

    @Test
    void expressCostsOneAndAHalfTimesStandard() {
        assertEquals(75.0, calculator.calculateCost("DOMESTIC", 10, "EXPRESS"), EXACT);
    }

    @Test
    void expressAlsoMultipliesTheZoneFeeAndHeavySurcharge() {
        assertEquals(967.5, calculator.calculateCost("INTERNATIONAL", 40, "EXPRESS"), EXACT);
    }

    @Test
    void shipmentOfExactlyThirtyKgPaysNoHeavySurcharge() {
        assertEquals(150.0, calculator.calculateCost("DOMESTIC", 30, "STANDARD"), EXACT);
    }

    @Test
    void shipmentOverThirtyKgPaysHeavySurcharge() {
        assertEquals(170.5, calculator.calculateCost("DOMESTIC", 30.1, "STANDARD"), EXACT);
    }
}
