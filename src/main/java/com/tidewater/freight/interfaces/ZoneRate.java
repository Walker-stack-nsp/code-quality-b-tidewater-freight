package com.tidewater.freight.interfaces;

import java.math.BigDecimal;

/**
 * The pricing rule for one zone.
 *
 * <p>This is the extension point for new zones. A zone priced like the existing ones is a new
 * {@link PerKilogramRate}; a zone with a different kind of rule implements this interface.
 * Neither change touches the calculator.
 */
public interface ZoneRate {

    /** The zone's cost for this weight, before the heavy-item surcharge and service level. */
    BigDecimal baseCost(BigDecimal weightKg);
}
