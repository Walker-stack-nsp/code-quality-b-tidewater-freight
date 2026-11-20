package com.tidewater.freight;

import com.tidewater.freight.enums.ServiceLevel;
import com.tidewater.freight.exceptions.InvalidShipmentException;
import com.tidewater.freight.exceptions.UnknownServiceLevelException;
import com.tidewater.freight.model.Shipment;
import com.tidewater.freight.model.Zone;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ShipmentTest {

    private static final Zone DOMESTIC = new Zone("DOMESTIC");

    @Test
    void textInputIsReadIntoTypedValues() {
        Shipment shipment = Shipment.of(" regional ", " 2.5 ", "express");

        assertEquals(new Zone("REGIONAL"), shipment.zone());
        assertEquals(new BigDecimal("2.5"), shipment.weightKg());
        assertEquals(ServiceLevel.EXPRESS, shipment.serviceLevel());
    }

    @Test
    void smallestPositiveWeightIsAccepted() {
        var weight = new BigDecimal("0.001");

        assertEquals(weight, new Shipment(DOMESTIC, weight, ServiceLevel.STANDARD).weightKg());
    }

    @Test
    void zeroWeightIsRejected() {
        assertThrows(InvalidShipmentException.class,
                () -> new Shipment(DOMESTIC, BigDecimal.ZERO, ServiceLevel.STANDARD));
    }

    @Test
    void negativeWeightIsRejected() {
        assertThrows(InvalidShipmentException.class,
                () -> new Shipment(DOMESTIC, new BigDecimal("-10"), ServiceLevel.STANDARD));
    }

    @Test
    void missingWeightIsRejected() {
        assertThrows(InvalidShipmentException.class,
                () -> new Shipment(DOMESTIC, null, ServiceLevel.STANDARD));
    }

    @Test
    void missingZoneIsRejected() {
        assertThrows(InvalidShipmentException.class,
                () -> new Shipment(null, BigDecimal.TEN, ServiceLevel.STANDARD));
    }

    @Test
    void missingServiceLevelIsRejected() {
        assertThrows(InvalidShipmentException.class,
                () -> new Shipment(DOMESTIC, BigDecimal.TEN, null));
    }

    @Test
    void weightThatIsNotANumberIsRejectedWithOriginalCause() {
        var refusal = assertThrows(InvalidShipmentException.class,
                () -> Shipment.of("DOMESTIC", "ten", "STANDARD"));

        assertInstanceOf(NumberFormatException.class, refusal.getCause());
    }

    @Test
    void blankWeightTextIsRejected() {
        assertThrows(InvalidShipmentException.class,
                () -> Shipment.of("DOMESTIC", " ", "STANDARD"));
    }

    @Test
    void blankZoneTextIsRejectedAsAnInvalidShipment() {
        assertThrows(InvalidShipmentException.class, () -> Shipment.of(" ", "10", "STANDARD"));
    }

    @Test
    void unknownServiceLevelTextIsRejected() {
        assertThrows(UnknownServiceLevelException.class,
                () -> Shipment.of("DOMESTIC", "10", "OVERNIGHT"));
    }
}
