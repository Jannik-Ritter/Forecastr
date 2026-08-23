package de.eva.forecastr.core.interfaces;

import de.eva.forecastr.core.models.AdminStats;
import de.eva.forecastr.core.models.Balance;
import de.eva.forecastr.core.models.Bet;
import de.eva.forecastr.core.models.ImportReport;
import de.eva.forecastr.core.models.LiveUpdate;
import de.eva.forecastr.core.models.Market;
import de.eva.forecastr.core.models.User;
import de.eva.forecastr.core.models.UserPage;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

public interface ForecastrGateway {
  void selectUser(long userId);

  List<User> users();

  UserPage userPage(int page, int size);

  User createUser(String username);

  User updateUser(long userId, String username);

  void deleteUser(long userId);

  Balance balance(long userId);

  Balance deposit(long userId, BigDecimal amount);

  Balance withdraw(long userId, BigDecimal amount);

  List<Market> feed();

  List<Market> search(String text, String status);

  Market event(long eventId);

  List<Bet> bets(long userId);

  Bet placeBet(long userId, long eventId, String outcome, BigDecimal stake);

  AdminStats adminStats();

  ImportReport importEvents(String path);

  AutoCloseable liveUpdates(long userId, Consumer<LiveUpdate> updates);
}
