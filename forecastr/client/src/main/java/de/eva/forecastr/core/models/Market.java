package de.eva.forecastr.core.models;

import java.math.BigDecimal;
import java.time.Instant;

public record Market(
    long id,
    String question,
    Instant createdAt,
    Instant closesAt,
    Instant settlementAt,
    String status,
    Instant resolvedAt,
    BigDecimal yesPool,
    BigDecimal noPool) {}
