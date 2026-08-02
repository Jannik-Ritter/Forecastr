package de.eva.forecastr.clients;

import de.eva.forecastr.core.models.Balance;
import de.eva.forecastr.core.models.User;

public final class ClientSession {
  private User user;
  private Balance balance;

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

  public void logout() {
    user = null;
    balance = null;
  }
}
