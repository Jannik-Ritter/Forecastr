package de.eva.forecastr.core.models;

import java.math.BigDecimal;

public record ManualResolutionResult(
    long eventId,
    String status,
    int winners,
    int losers,
    BigDecimal payouts,
    BigDecimal fees,
    boolean changed) {}
