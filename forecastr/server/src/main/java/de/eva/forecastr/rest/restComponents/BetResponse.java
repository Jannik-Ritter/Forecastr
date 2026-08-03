package de.eva.forecastr.rest.restComponents;

import de.eva.forecastr.core.models.BetStatus;
import de.eva.forecastr.core.models.Outcome;
import java.math.BigDecimal;
import java.time.Instant;

public record BetResponse(
    Long id,
    Long userId,
    Long eventId,
    Outcome outcome,
    BigDecimal stake,
    Instant placedAt,
    BetStatus status,
    BigDecimal payoutAmount,
    BigDecimal feeAmount) {}
