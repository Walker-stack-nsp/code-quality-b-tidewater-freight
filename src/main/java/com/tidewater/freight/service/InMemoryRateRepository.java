package com.tidewater.freight.service;

import com.tidewater.freight.interfaces.RateRepository;
import com.tidewater.freight.interfaces.ZoneRate;
import com.tidewater.freight.model.PerKilogramRate;
import com.tidewater.freight.model.Zone;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

public class InMemoryRateRepository implements RateRepository {

    private final Map<Zone, ZoneRate> ratesByZone;

    public InMemoryRateRepository(Map<Zone, ZoneRate> ratesByZone) {
        this.ratesByZone = Map.copyOf(ratesByZone);
    }

    /** Tidewater's current rate table: one entry per zone, each value named by its parameter. */
    public static InMemoryRateRepository withStandardRates() {
        return new InMemoryRateRepository(Map.of(
                new Zone("DOMESTIC"),
                new PerKilogramRate(new BigDecimal("5.00"), BigDecimal.ZERO),
                new Zone("REGIONAL"),
                new PerKilogramRate(new BigDecimal("8.00"), new BigDecimal("10.00")),
                new Zone("INTERNATIONAL"),
                new PerKilogramRate(new BigDecimal("15.00"), new BigDecimal("25.00"))));
    }

    @Override
    public Optional<ZoneRate> findRate(Zone zone) {
        return Optional.ofNullable(ratesByZone.get(zone));
    }
}
