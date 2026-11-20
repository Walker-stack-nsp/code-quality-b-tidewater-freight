package com.tidewater.freight;

import com.tidewater.freight.interfaces.ZoneRate;
import com.tidewater.freight.model.PerKilogramRate;
import com.tidewater.freight.model.Zone;
import com.tidewater.freight.service.InMemoryRateRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InMemoryRateRepositoryTest {

    private static final Zone DOMESTIC = new Zone("DOMESTIC");
    private static final ZoneRate DOMESTIC_RATE =
            new PerKilogramRate(new BigDecimal("5.00"), BigDecimal.ZERO);

    @Test
    void knownZoneReturnsItsRate() {
        var repository = new InMemoryRateRepository(Map.of(DOMESTIC, DOMESTIC_RATE));

        assertEquals(Optional.of(DOMESTIC_RATE), repository.findRate(new Zone("domestic")));
    }

    @Test
    void unknownZoneReturnsNothing() {
        var repository = new InMemoryRateRepository(Map.of(DOMESTIC, DOMESTIC_RATE));

        assertEquals(Optional.empty(), repository.findRate(new Zone("MOON")));
    }

    @Test
    void laterChangesToTheSuppliedTableDoNotChangeTheRates() {
        var table = new HashMap<Zone, ZoneRate>();
        table.put(DOMESTIC, DOMESTIC_RATE);
        var repository = new InMemoryRateRepository(table);

        table.put(new Zone("MOON"), DOMESTIC_RATE);

        assertEquals(Optional.empty(), repository.findRate(new Zone("MOON")));
    }
}
