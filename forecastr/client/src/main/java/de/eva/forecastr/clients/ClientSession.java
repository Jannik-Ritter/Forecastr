package de.eva.forecastr.clients;

import de.eva.forecastr.core.models.Balance;
import de.eva.forecastr.core.models.Market;
import de.eva.forecastr.core.models.User;
import java.util.HashMap;
import java.util.Map;

public final class ClientSession {
  private final Map<Long, Market> eventsById;
  private User user;
  private Balance balance;

  public ClientSession() {
    this.eventsById = new HashMap<>();
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = user;
  }

  public Balance balance() {
    return balance;
  }

  public void balance(Balance balance) {
    this.balance = balance;
  }

  public Market event(long eventId) {
    return eventsById.get(eventId);
  }

  public void remember(Market event) {
    eventsById.put(event.id(), event);
  }

  public void logout() {
    user = null;
    balance = null;
  }
}
