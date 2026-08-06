package de.eva.forecastr.rest.restComponents;

import de.eva.forecastr.core.models.EventStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record EventResponse(
    Long id,
    String question,
    Instant createdAt,
    Instant closesAt,
    Instant settlementAt,
    EventStatus status,
    Instant resolvedAt,
    BigDecimal yesPool,
    BigDecimal noPool) {}
