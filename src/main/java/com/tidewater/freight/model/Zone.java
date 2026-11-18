package com.tidewater.freight.model;

import java.util.Locale;

public record Zone(String code) {

    public Zone {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Zone code must not be blank");
        }
        code = code.strip().toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return code;
    }
}
