package com.tidewater.freight;

import com.tidewater.freight.enums.ServiceLevel;
import com.tidewater.freight.exceptions.UnknownZoneException;
import com.tidewater.freight.interfaces.RateRepository;
import com.tidewater.freight.interfaces.ZoneRate;
import com.tidewater.freight.model.Shipment;
import com.tidewater.freight.model.Zone;
import com.tidewater.freight.service.ShippingCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShippingCalculatorTest {

    private static final Zone ZONE = new Zone("TEST");
    private static final ZoneRate FORTY_CEDIS = weightKg -> new BigDecimal("40.00");

    private final RateRepository rateRepository = mock(RateRepository.class);
    private final ShippingCalculator calculator = new ShippingCalculator(rateRepository);

    @Test
    void standardShipmentCostsTheZoneBaseCost() {
        givenZoneRate(FORTY_CEDIS);

        assertEquals(new BigDecimal("40.00"), calculator.calculateCost(shipment("10")));
    }

    @Test
    void shipmentWeightIsPassedToTheZoneRate() {
        givenZoneRate(weightKg -> weightKg);

        assertEquals(new BigDecimal("12.50"), calculator.calculateCost(shipment("12.5")));
    }

    @Test
    void expressCostsOneAndAHalfTimesTheBaseCost() {
        givenZoneRate(FORTY_CEDIS);

        assertEquals(new BigDecimal("60.00"), calculator.calculateCost(expressShipment("10")));
    }

    @Test
    void shipmentOverThirtyKgPaysTheHeavyItemSurcharge() {
        givenZoneRate(FORTY_CEDIS);

        assertEquals(new BigDecimal("60.00"), calculator.calculateCost(shipment("30.01")));
    }

    @Test
    void shipmentOfExactlyThirtyKgPaysNoHeavyItemSurcharge() {
        givenZoneRate(FORTY_CEDIS);

        assertEquals(new BigDecimal("40.00"), calculator.calculateCost(shipment("30")));
    }

    @Test
    void expressAlsoMultipliesTheHeavyItemSurcharge() {
        givenZoneRate(FORTY_CEDIS);

        assertEquals(new BigDecimal("90.00"), calculator.calculateCost(expressShipment("31")));
    }

    @Test
    void priceIsRoundedHalfUpToPesewas() {
        givenZoneRate(weightKg -> new BigDecimal("10.005"));

        assertEquals(new BigDecimal("10.01"), calculator.calculateCost(shipment("1")));
    }

    @Test
    void newZoneWithItsOwnKindOfRuleNeedsNoCalculatorChange() {
        ZoneRate minimumCharge =
                weightKg -> weightKg.multiply(new BigDecimal("2.00")).max(new BigDecimal("25.00"));
        givenZoneRate(minimumCharge);

        assertEquals(new BigDecimal("25.00"), calculator.calculateCost(shipment("5")));
        assertEquals(new BigDecimal("40.00"), calculator.calculateCost(shipment("20")));
    }

    @Test
    void unknownZoneIsRefusedInsteadOfPricedAtZero() {
        when(rateRepository.findRate(ZONE)).thenReturn(Optional.empty());

        var refusal = assertThrows(UnknownZoneException.class,
                () -> calculator.calculateCost(shipment("10")));

        assertTrue(refusal.getMessage().contains("TEST"));
    }

    @Test
    void rateSourceFailureReachesTheCallerUnchanged() {
        var outage = new IllegalStateException("Rate database unavailable");
        when(rateRepository.findRate(ZONE)).thenThrow(outage);

        var thrown = assertThrows(IllegalStateException.class,
                () -> calculator.calculateCost(shipment("10")));

        assertSame(outage, thrown);
    }

    @Test
    void missingRateRepositoryIsRejected() {
        assertThrows(NullPointerException.class, () -> new ShippingCalculator(null));
    }

    private void givenZoneRate(ZoneRate rate) {
        when(rateRepository.findRate(ZONE)).thenReturn(Optional.of(rate));
    }

    private static Shipment shipment(String weightKg) {
        return new Shipment(ZONE, new BigDecimal(weightKg), ServiceLevel.STANDARD);
    }

    private static Shipment expressShipment(String weightKg) {
        return new Shipment(ZONE, new BigDecimal(weightKg), ServiceLevel.EXPRESS);
    }
}
