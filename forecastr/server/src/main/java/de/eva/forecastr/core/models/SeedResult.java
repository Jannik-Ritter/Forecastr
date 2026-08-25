package de.eva.forecastr.core.models;

import java.util.List;

public record SeedResult(List<Long> userIds, List<Long> eventIds) {
  public SeedResult {
    userIds = List.copyOf(userIds);
    eventIds = List.copyOf(eventIds);
  }
}
