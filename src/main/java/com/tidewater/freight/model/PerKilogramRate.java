package com.tidewater.freight.model;

import com.tidewater.freight.interfaces.ZoneRate;

import java.math.BigDecimal;

public record PerKilogramRate(BigDecimal ratePerKg, BigDecimal handlingFee) implements ZoneRate {

    public PerKilogramRate {
        requireNotNegative(ratePerKg, "ratePerKg");
        requireNotNegative(handlingFee, "handlingFee");
    }

    @Override
    public BigDecimal baseCost(BigDecimal weightKg) {
        return ratePerKg.multiply(weightKg).add(handlingFee);
    }

    private static void requireNotNegative(BigDecimal amount, String field) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException(field + " must be zero or more, got " + amount);
        }
    }
}
