package com.tidewater.freight.exceptions;

/** Thrown when a shipment's details are missing or impossible, such as a weight of zero or less. */
public class InvalidShipmentException extends ShippingException {

    public InvalidShipmentException(String message) {
        super(message);
    }

    public InvalidShipmentException(String message, Throwable cause) {
        super(message, cause);
    }
}
