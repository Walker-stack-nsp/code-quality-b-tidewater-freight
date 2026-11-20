package com.tidewater.freight.exceptions;

/**
 * Parent of every reason a shipping quote is refused.
 *
 * <p>Unchecked, because a refusal is handled once, where the request came in, and every layer in
 * between would otherwise have to declare it. Abstract, because callers should always see the
 * specific reason.
 */
public abstract class ShippingException extends RuntimeException {

    protected ShippingException(String message) {
        super(message);
    }

    protected ShippingException(String message, Throwable cause) {
        super(message, cause);
    }
}
