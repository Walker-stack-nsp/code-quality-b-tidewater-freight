package com.tidewater.freight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidewater.freight.model.Quote;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class QuoteTest {

    @Test
    void quoteWhoseLinesAddUpIsAccepted() {
        var quote = new Quote(new BigDecimal("625.00"), new BigDecimal("20.00"),
                new BigDecimal("322.50"), new BigDecimal("967.50"));

        assertEquals(new BigDecimal("967.50"), quote.total());
    }

    @Test
    void quoteWhoseLinesDoNotAddUpIsRejected() {
        var refusal = assertThrows(IllegalArgumentException.class,
                () -> new Quote(new BigDecimal("625.00"), new BigDecimal("20.00"),
                        new BigDecimal("322.50"), new BigDecimal("967.51")));

        assertTrue(refusal.getMessage().contains("967.51"));
    }

    @Test
    void serviceLevelChargeCountsTowardsTheTotal() {
        assertThrows(IllegalArgumentException.class,
                () -> new Quote(new BigDecimal("625.00"), new BigDecimal("20.00"),
                        new BigDecimal("322.50"), new BigDecimal("645.00")));
    }

    @Test
    void amountsAreComparedByValueNotByDecimalPlaces() {
        var quote = new Quote(new BigDecimal("28.40"), BigDecimal.ZERO,
                new BigDecimal("14.20"), new BigDecimal("42.6"));

        assertEquals(new BigDecimal("42.6"), quote.total());
    }

    @Test
    void missingLineIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new Quote(null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }
}