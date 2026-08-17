package de.eva.forecastr.core.models;

import java.math.BigDecimal;
import java.time.Instant;

public record Bet(
    long id,
    long userId,
    long eventId,
    String outcome,
    BigDecimal stake,
    Instant placedAt,
    String status,
    BigDecimal payoutAmount,
    BigDecimal feeAmount) {}
