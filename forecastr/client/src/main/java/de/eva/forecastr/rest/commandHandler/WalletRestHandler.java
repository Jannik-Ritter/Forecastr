package de.eva.forecastr.rest.commandHandler;

import de.eva.forecastr.core.models.Balance;
import de.eva.forecastr.rest.CommunicationHandler;
import java.math.BigDecimal;
import java.util.Map;

public final class WalletRestHandler {
  private final CommunicationHandler communicationHandler;

  public WalletRestHandler(CommunicationHandler communicationHandler) {
    this.communicationHandler = communicationHandler;
  }

  public Balance getBalance(long userId) {
    return communicationHandler.value(
        communicationHandler.get("/users/" + userId + "/balance"), Balance.class);
  }

  public Balance deposit(long userId, BigDecimal amount) {
    return updateBalance(userId, "deposit", amount);
  }

  public Balance withdraw(long userId, BigDecimal amount) {
    return updateBalance(userId, "withdraw", amount);
  }

  private Balance updateBalance(long userId, String operation, BigDecimal amount) {
    return communicationHandler.value(
        communicationHandler.post("/users/" + userId + "/" + operation, Map.of("amount", amount)),
        Balance.class);
  }
}
