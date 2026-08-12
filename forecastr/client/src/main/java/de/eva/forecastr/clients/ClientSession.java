package de.eva.forecastr.clients;

import de.eva.forecastr.core.models.Balance;
import de.eva.forecastr.core.models.Market;
import de.eva.forecastr.core.models.User;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class ClientSession {
  private final Queue<String> notifications;
  private final Map<Long, Market> eventsById;
  private User user;
  private Balance balance;

  public ClientSession() {
    this.notifications = new ConcurrentLinkedQueue<>();
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

  public void notify(String message) {
    notifications.add(message);
  }

  public String nextNotification() {
    return notifications.poll();
  }

  public void logout() {
    user = null;
    balance = null;
    notifications.clear();
  }
}
