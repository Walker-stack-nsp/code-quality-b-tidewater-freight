package com.tidewater.freight.model;

import java.math.BigDecimal;

/** A price broken down into the rules that produced it. The lines always add up to the total. */
public record Quote(BigDecimal zoneCost, BigDecimal heavyItemSurcharge,
                    BigDecimal serviceLevelCharge, BigDecimal total) {

    public Quote {
        BigDecimal sumOfLines = zoneCost.add(heavyItemSurcharge).add(serviceLevelCharge);
        if (sumOfLines.compareTo(total) != 0) {
            throw new IllegalArgumentException(
                    "Quote lines add up to " + sumOfLines + " but the total is " + total);
        }
    }
}