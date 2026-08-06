package de.eva.forecastr.rest;

import de.eva.forecastr.core.interfaces.ForecastrGateway;
import de.eva.forecastr.core.models.Balance;
import de.eva.forecastr.core.models.Market;
import de.eva.forecastr.core.models.User;
import de.eva.forecastr.core.models.UserPage;
import de.eva.forecastr.rest.commandHandler.EventRestHandler;
import de.eva.forecastr.rest.commandHandler.UserRestHandler;
import de.eva.forecastr.rest.commandHandler.WalletRestHandler;
import java.math.BigDecimal;
import java.util.List;

public final class RestForecastrGateway implements ForecastrGateway {
  private final UserRestHandler userHandler;
  private final WalletRestHandler walletHandler;
  private final EventRestHandler eventHandler;

  public RestForecastrGateway(CommunicationHandler communicationHandler) {
    this.userHandler = new UserRestHandler(communicationHandler);
    this.walletHandler = new WalletRestHandler(communicationHandler);
    this.eventHandler = new EventRestHandler(communicationHandler);
  }

  @Override public void selectUser(long userId) {}
  @Override public List<User> users() { return userHandler.getUsers(); }
  @Override public UserPage userPage(int page, int size) { return userHandler.getUserPage(page, size); }
  @Override public User createUser(String username) { return userHandler.createUser(username); }
  @Override public User updateUser(long userId, String username) { return userHandler.updateUser(userId, username); }
  @Override public void deleteUser(long userId) { userHandler.deleteUser(userId); }
  @Override public Balance balance(long userId) { return walletHandler.getBalance(userId); }
  @Override public Balance deposit(long userId, BigDecimal amount) { return walletHandler.deposit(userId, amount); }
  @Override public Balance withdraw(long userId, BigDecimal amount) { return walletHandler.withdraw(userId, amount); }
  @Override public List<Market> feed() { return eventHandler.getFeed(); }
  @Override public List<Market> search(String text, String status) { return eventHandler.searchEvents(text, status); }
  @Override public Market event(long eventId) { return eventHandler.getEvent(eventId); }
}
