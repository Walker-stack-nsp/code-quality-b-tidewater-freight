package com.tidewater.freight.enums;

import com.tidewater.freight.exceptions.UnknownServiceLevelException;

import java.math.BigDecimal;
import java.util.Locale;

public enum ServiceLevel {

    STANDARD(new BigDecimal("1.0")),
    EXPRESS(new BigDecimal("1.5"));

    private final BigDecimal priceMultiplier;

    ServiceLevel(BigDecimal priceMultiplier) {
        this.priceMultiplier = priceMultiplier;
    }

    /**
     * Reads a service level from text such as "express".
     *
     * @throws UnknownServiceLevelException if the code is missing or matches no service level
     */
    public static ServiceLevel fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new UnknownServiceLevelException(code);
        }
        try {
            return valueOf(code.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new UnknownServiceLevelException(code, e);
        }
    }

    public BigDecimal applyTo(BigDecimal cost) {
        return cost.multiply(priceMultiplier);
    }
}
