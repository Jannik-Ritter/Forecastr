package de.eva.forecastr.rest.restComponents;

import java.util.List;

public record ResolutionAuditResponse(EventResponse event, List<BetResponse> bets) {
  public ResolutionAuditResponse {
    bets = List.copyOf(bets);
  }
}
