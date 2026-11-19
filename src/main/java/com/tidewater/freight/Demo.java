package com.tidewater.freight;

import com.tidewater.freight.exceptions.ShippingException;
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
            BigDecimal cost = calculator.calculateCost(Shipment.of(zone, weightKg, serviceLevel));
            System.out.println(request + " -> GHS " + cost);
        } catch (ShippingException refusal) {
            System.out.println(request + " -> refused: " + refusal.getMessage());
        }
    }
}
