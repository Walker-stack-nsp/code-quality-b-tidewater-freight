package com.tidewater.freight;

import com.tidewater.freight.exceptions.ShippingException;
import com.tidewater.freight.model.Quote;
import com.tidewater.freight.model.Shipment;
import com.tidewater.freight.service.InMemoryRateRepository;
import com.tidewater.freight.service.ShippingCalculator;

import java.math.BigDecimal;

public class Demo {
    public static void main(String[] args) {
        var calculator = new ShippingCalculator(InMemoryRateRepository.withStandardRates());

        quote(calculator, "DOMESTIC", "10", "STANDARD");
        quote(calculator, "INTERNATIONAL", "40", "EXPRESS");
        quote(calculator, "MOON", "10", "STANDARD");
        quote(calculator, "DOMESTIC", "-10", "STANDARD");
        quote(calculator, "DOMESTIC", "10", "OVERNIGHT");
    }

    private static void quote(ShippingCalculator calculator, String zone, String weightKg,
                              String serviceLevel) {
        String request = zone + " " + weightKg + "kg " + serviceLevel;
        try {
            Quote quote = calculator.calculateQuote(Shipment.of(zone, weightKg, serviceLevel));
            System.out.println(request + " -> GHS " + quote.total());
            printBreakdown(quote);
        } catch (ShippingException refusal) {
            System.out.println(request + " -> refused: " + refusal.getMessage());
        }
    }

    private static void printBreakdown(Quote quote) {
        System.out.printf("    %-22s %8s%n", "zone cost", quote.zoneCost());
        System.out.printf("    %-22s %8s%n", "heavy-item surcharge", quote.heavyItemSurcharge());
        System.out.printf("    %-22s %8s%n", "service-level charge", quote.serviceLevelCharge());
    }
}
