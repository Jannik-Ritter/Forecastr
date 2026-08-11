package de.eva.forecastr.core.models;

import java.math.BigDecimal;

public record ResolutionResult(
    Long eventId,
    EventStatus status,
    int winners,
    int losers,
    BigDecimal payouts,
    BigDecimal fees,
    boolean changed) {
  public static ResolutionResult unchanged(Long id, EventStatus status) {
    return new ResolutionResult(id, status, 0, 0, Money.ZERO, Money.ZERO, false);
  }
}
