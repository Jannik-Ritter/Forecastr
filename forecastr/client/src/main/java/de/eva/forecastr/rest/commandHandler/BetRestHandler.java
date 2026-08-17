package de.eva.forecastr.rest.commandHandler;

import de.eva.forecastr.core.models.Bet;
import de.eva.forecastr.rest.CommunicationHandler;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class BetRestHandler {
  private final CommunicationHandler communicationHandler;

  public BetRestHandler(CommunicationHandler communicationHandler) {
    this.communicationHandler = communicationHandler;
  }

  public List<Bet> getBets(long userId) {
    return communicationHandler.values(
        communicationHandler.get("/users/" + userId + "/bets"), Bet.class);
  }

  public Bet placeBet(long userId, long eventId, String outcome, BigDecimal stake) {
    return communicationHandler.value(
        communicationHandler.post(
            "/events/" + eventId + "/bets",
            Map.of(
                "userId", userId,
                "outcome", outcome,
                "stake", stake)),
        Bet.class);
  }
}
