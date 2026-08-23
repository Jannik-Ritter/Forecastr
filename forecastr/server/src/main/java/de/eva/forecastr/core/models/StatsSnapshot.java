package de.eva.forecastr.core.models;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record StatsSnapshot(
    long users,
    long events,
    Map<String, Long> eventsByStatus,
    long bets,
    BigDecimal payouts,
    BigDecimal fees,
    Map<Long, Long> perUserBetCount,
    Map<String, Long> resolver) {
  public StatsSnapshot {
    eventsByStatus = immutableCopy(eventsByStatus);
    perUserBetCount = immutableCopy(perUserBetCount);
    resolver = immutableCopy(resolver);
  }

  private static <K, V> Map<K, V> immutableCopy(Map<K, V> values) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(values));
  }
}
