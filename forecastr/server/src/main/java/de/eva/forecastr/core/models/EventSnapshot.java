package de.eva.forecastr.core.models;

import java.math.BigDecimal;
import java.time.Instant;

public record EventSnapshot(
    Long id,
    String question,
    Instant createdAt,
    Instant closesAt,
    Instant settlementAt,
    EventStatus status,
    Instant resolvedAt,
    BigDecimal yesPool,
    BigDecimal noPool) {}
