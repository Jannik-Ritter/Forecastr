package de.eva.forecastr.core.models;

import java.util.List;

public record TestUsersResponse(List<Long> userIds, List<Long> betIds) {
  public TestUsersResponse {
    userIds = List.copyOf(userIds);
    betIds = List.copyOf(betIds);
  }
}
