package com.tidewater.freight.exceptions;

import com.tidewater.freight.model.Zone;

public class UnknownZoneException extends ShippingException{

    public UnknownZoneException(Zone zone) {
        super("No rates are configured for zone '" + zone + "'");
    }
}
