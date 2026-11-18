package com.tidewater.freight.interfaces;

import com.tidewater.freight.model.Zone;

import java.util.Optional;

/**
 * Where zone rates come from.
 *
 * <p>Returns an empty result for a zone it does not know, and leaves the decision about what that
 * means to the caller.
 */
public interface RateRepository {

    Optional<ZoneRate> findRate(Zone zone);
}
