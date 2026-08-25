package de.eva.forecastr.core.models;

import java.util.List;

public record TestEventsResult(List<Long> eventIds) {
  public TestEventsResult {
    eventIds = List.copyOf(eventIds);
  }
}
