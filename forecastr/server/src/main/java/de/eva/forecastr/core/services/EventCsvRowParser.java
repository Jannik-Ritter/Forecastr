package de.eva.forecastr.core.services;

import de.eva.forecastr.core.models.MarketEvent;
import java.time.Instant;
import java.util.Map;

public final class EventCsvRowParser {
  public MarketEvent parse(Map<String, String> row, Instant importedAt) {
    required(row, "id");
    required(row, "question");
    required(row, "createdOffsetMin");
    required(row, "closesOffsetMin");
    required(row, "plannedResolution");
    throw new UnsupportedOperationException("offset conversion is not implemented yet");
  }

  private static String required(Map<String, String> row, String name) {
    String value = row.get(name);
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " is required");
    }
    return value;
  }
}
