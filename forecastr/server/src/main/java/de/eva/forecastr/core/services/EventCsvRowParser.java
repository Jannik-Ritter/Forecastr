package de.eva.forecastr.core.services;

import de.eva.forecastr.core.models.MarketEvent;
import de.eva.forecastr.core.models.PlannedResolution;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public final class EventCsvRowParser {
  public MarketEvent parse(Map<String, String> row, Instant importedAt) {
    try {
      long id = positiveLong(row, "id");
      String question = required(row, "question");
      if (!question.trim().endsWith("?"))
        throw new IllegalArgumentException("question must be binary and end with '?'");
      long createdOffset = longValue(row, "createdOffsetMin");
      long closesOffset = longValue(row, "closesOffsetMin");
      if (createdOffset < 0 || closesOffset <= createdOffset)
        throw new IllegalArgumentException(
            "offsets must be non-negative and closing must follow creation");
      if (closesOffset - createdOffset > 1440)
        throw new IllegalArgumentException("event lifetime exceeds 24 hours");
      PlannedResolution planned =
          PlannedResolution.valueOf(required(row, "plannedResolution").toUpperCase());
      String rawResolutionOffset = row.getOrDefault("plannedResolutionOffsetMin", "").trim();
      if ((planned == PlannedResolution.UNRESOLVABLE) != rawResolutionOffset.isEmpty())
        throw new IllegalArgumentException("resolution offset must be empty iff UNRESOLVABLE");
      Instant createdAt = importedAt.plus(createdOffset, ChronoUnit.MINUTES);
      Instant closesAt = importedAt.plus(closesOffset, ChronoUnit.MINUTES);
      Instant plannedAt = null;
      if (planned != PlannedResolution.UNRESOLVABLE) {
        long resolutionOffset = Long.parseLong(rawResolutionOffset);
        if (resolutionOffset < createdOffset || resolutionOffset > closesOffset)
          throw new IllegalArgumentException("resolution offset must be within event lifetime");
        plannedAt = importedAt.plus(resolutionOffset, ChronoUnit.MINUTES);
      }
      return new MarketEvent(id, question.trim(), createdAt, closesAt, planned, plannedAt);
    } catch (NumberFormatException | NullPointerException e) {
      throw new IllegalArgumentException("invalid numeric or enum value", e);
    }
  }

  private static String required(Map<String, String> row, String name) {
    String value = row.get(name);
    if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
    return value;
  }

  private static long longValue(Map<String, String> row, String name) {
    return Long.parseLong(required(row, name));
  }

  private static long positiveLong(Map<String, String> row, String name) {
    long value = longValue(row, name);
    if (value <= 0) {
      throw new IllegalArgumentException(name + " must be positive");
    }
    return value;
  }
}
