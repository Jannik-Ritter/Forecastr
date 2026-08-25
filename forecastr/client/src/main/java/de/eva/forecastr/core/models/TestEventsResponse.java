package de.eva.forecastr.core.models;

import java.util.List;

public record TestEventsResponse(List<Long> eventIds) {
  public TestEventsResponse {
    eventIds = List.copyOf(eventIds);
  }
}
