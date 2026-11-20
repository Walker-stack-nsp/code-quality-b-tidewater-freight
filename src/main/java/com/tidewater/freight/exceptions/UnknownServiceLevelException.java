package com.tidewater.freight.exceptions;

import com.tidewater.freight.enums.ServiceLevel;

import java.util.Arrays;

/** Thrown when a service level code does not match any {@link ServiceLevel}. */
public class UnknownServiceLevelException extends ShippingException {

    public UnknownServiceLevelException(String message) {
        super(messageFor(message));
    }

    public UnknownServiceLevelException(String code, Throwable cause) {
        super(messageFor(code), cause);
    }

    private static String messageFor(String code) {
        return "Unknown service level '" + code + "'; expected one of "
                + Arrays.toString(ServiceLevel.values());
    }
}
