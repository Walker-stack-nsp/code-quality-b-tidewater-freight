package com.tidewater.freight.service;

import com.tidewater.freight.exceptions.UnknownZoneException;
import com.tidewater.freight.interfaces.RateRepository;
import com.tidewater.freight.interfaces.ZoneRate;
import com.tidewater.freight.model.Quote;
import com.tidewater.freight.model.Shipment;
import com.tidewater.freight.model.Zone;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Works out the price of a shipment.
 *
 * <p>The zone's own rule comes from the {@link RateRepository}. The calculator then applies the
 * rules that are the same for every zone: the heavy-item surcharge, the service-level
 * multiplier, and rounding to pesewas.
 */
public class ShippingCalculator {

    private static final BigDecimal HEAVY_ITEM_THRESHOLD_KG = new BigDecimal("30");
    private static final BigDecimal HEAVY_ITEM_SURCHARGE = new BigDecimal("20.00");
    private static final int MONEY_SCALE = 2;

    private final RateRepository rateRepository;

    public ShippingCalculator(RateRepository rateRepository) {
        this.rateRepository = Objects.requireNonNull(rateRepository, "rateRepository");
    }

    /**
     * Returns the price in GHS, rounded to 2 decimal places.
     *
     * @throws UnknownZoneException if no rates are configured for the shipment's zone
     */
    public BigDecimal calculateCost(Shipment shipment) {
        return calculateQuote(shipment).total();
    }

    private ZoneRate rateFor(Zone zone) {
        return rateRepository.findRate(zone)
                .orElseThrow(() -> new UnknownZoneException(zone));
    }

    private BigDecimal heavyItemSurcharge(BigDecimal weightKg) {
        if (weightKg.compareTo(HEAVY_ITEM_THRESHOLD_KG) > 0) {
            return HEAVY_ITEM_SURCHARGE;
        }
        return BigDecimal.ZERO;
    }

    public Quote calculateQuote(Shipment shipment) {
        Objects.requireNonNull(shipment, "shipment");
        BigDecimal zoneCost = rateFor(shipment.zone()).baseCost(shipment.weightKg());
        BigDecimal surcharge = heavyItemSurcharge(shipment.weightKg());
        BigDecimal total = toPesewas(shipment.serviceLevel().applyTo(zoneCost.add(surcharge)));
        BigDecimal zoneLine = toPesewas(zoneCost);
        return new Quote(zoneLine, surcharge, total.subtract(zoneLine).subtract(surcharge), total);
    }

    private BigDecimal toPesewas(BigDecimal price){
        return price.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
