package de.eva.forecastr.core.models;

import java.util.List;

public record TestUsersResult(List<Long> userIds, List<Long> betIds) {
  public TestUsersResult {
    userIds = List.copyOf(userIds);
    betIds = List.copyOf(betIds);
  }
}
