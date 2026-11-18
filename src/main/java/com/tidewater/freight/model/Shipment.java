package com.tidewater.freight.model;

import com.tidewater.freight.enums.ServiceLevel;
import com.tidewater.freight.exceptions.InvalidShipmentException;
import com.tidewater.freight.exceptions.UnknownServiceLevelException;

import java.math.BigDecimal;

public record Shipment(Zone zone, BigDecimal weightKg, ServiceLevel serviceLevel) {

    public Shipment {
        if (zone == null) {
            throw new InvalidShipmentException("Zone is required");
        }
        if (serviceLevel == null) {
            throw new InvalidShipmentException("Service level is required");
        }
        if (weightKg == null || weightKg.signum() <= 0) {
            throw new InvalidShipmentException("Weight must be greater than 0 kg, got " + weightKg);
        }
    }

    /**
     * Builds a shipment from text, as it arrives from a form or another system.
     *
     * @throws InvalidShipmentException if the zone is blank or the weight is not a positive
     *     number
     * @throws UnknownServiceLevelException if the service level is not recognised
     */
    public static Shipment of(String zoneCode, String weightKg, String serviceLevelCode) {
        if (zoneCode == null || zoneCode.isBlank()) {
            throw new InvalidShipmentException("Zone is required");
        }
        return new Shipment(new Zone(zoneCode), parseWeight(weightKg),
                ServiceLevel.fromCode(serviceLevelCode));
    }

    private static BigDecimal parseWeight(String weightKg) {
        if (weightKg == null || weightKg.isBlank()) {
            throw new InvalidShipmentException("Weight is required");
        }
        try {
            return new BigDecimal(weightKg.strip());
        } catch (NumberFormatException e) {
            throw new InvalidShipmentException("Weight is not a number: '" + weightKg + "'", e);
        }
    }
}
