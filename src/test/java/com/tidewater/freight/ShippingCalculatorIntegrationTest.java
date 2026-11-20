package com.tidewater.freight;

import com.tidewater.freight.exceptions.UnknownZoneException;
import com.tidewater.freight.model.Quote;
import com.tidewater.freight.model.Shipment;
import com.tidewater.freight.service.InMemoryRateRepository;
import com.tidewater.freight.service.ShippingCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ShippingCalculatorIntegrationTest {

    private final ShippingCalculator calculator =
            new ShippingCalculator(InMemoryRateRepository.withStandardRates());

    @Test
    void domesticStandardCostsRatePerKgTimesWeight() {
        var shipment = Shipment.of("DOMESTIC", "10", "STANDARD");

        assertEquals(new BigDecimal("50.00"), calculator.calculateCost(shipment));
    }

    @Test
    void regionalAddsItsHandlingFee() {
        var shipment = Shipment.of("REGIONAL", "5", "STANDARD");

        assertEquals(new BigDecimal("50.00"), calculator.calculateCost(shipment));
    }

    @Test
    void internationalAddsItsCustomsHandlingFee() {
        var shipment = Shipment.of("INTERNATIONAL", "10", "STANDARD");

        assertEquals(new BigDecimal("175.00"), calculator.calculateCost(shipment));
    }

    @Test
    void expressCostsOneAndAHalfTimesStandard() {
        var shipment = Shipment.of("DOMESTIC", "10", "EXPRESS");

        assertEquals(new BigDecimal("75.00"), calculator.calculateCost(shipment));
    }

    @Test
    void expressAlsoMultipliesTheZoneFeeAndHeavySurcharge() {
        var shipment = Shipment.of("INTERNATIONAL", "40", "EXPRESS");

        assertEquals(new BigDecimal("967.50"), calculator.calculateCost(shipment));
    }

    @Test
    void shipmentOfExactlyThirtyKgPaysNoHeavySurcharge() {
        var shipment = Shipment.of("DOMESTIC", "30", "STANDARD");

        assertEquals(new BigDecimal("150.00"), calculator.calculateCost(shipment));
    }

    @Test
    void shipmentOverThirtyKgPaysHeavySurcharge() {
        var shipment = Shipment.of("DOMESTIC", "30.1", "STANDARD");

        assertEquals(new BigDecimal("170.50"), calculator.calculateCost(shipment));
    }

    @Test
    void fractionalWeightGivesAnExactPriceInPesewas() {
        var shipment = Shipment.of("REGIONAL", "2.3", "EXPRESS");

        assertEquals(new BigDecimal("42.60"), calculator.calculateCost(shipment));
    }

    @Test
    void zoneCodeIsNotCaseSensitive() {
        var shipment = Shipment.of("domestic", "10", "standard");

        assertEquals(new BigDecimal("50.00"), calculator.calculateCost(shipment));
    }

    @Test
    void zoneWithNoConfiguredRatesIsRefused() {
        var shipment = Shipment.of("MOON", "10", "STANDARD");

        assertThrows(UnknownZoneException.class, () -> calculator.calculateCost(shipment));
    }


    @Test
    void heavyExpressInternationalQuoteIsItemised() {
        var shipment = Shipment.of("INTERNATIONAL", "40", "EXPRESS");

        var expected = new Quote(new BigDecimal("625.00"), new BigDecimal("20.00"),
                new BigDecimal("322.50"), new BigDecimal("967.50"));
        assertEquals(expected, calculator.calculateQuote(shipment));
    }

    @Test
    void fractionalWeightQuoteAddsUpToTheExactPrice() {
        var shipment = Shipment.of("REGIONAL", "2.3", "EXPRESS");

        var expected = new Quote(new BigDecimal("28.40"), new BigDecimal("0.00"),
                new BigDecimal("14.20"), new BigDecimal("42.60"));
        assertEquals(expected, calculator.calculateQuote(shipment));
    }
}
