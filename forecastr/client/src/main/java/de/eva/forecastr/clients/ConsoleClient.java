package de.eva.forecastr.clients;

import de.eva.forecastr.clients.commandHandler.AdminCommandHandler;
import de.eva.forecastr.clients.commandHandler.BetCommandHandler;
import de.eva.forecastr.clients.commandHandler.EventCommandHandler;
import de.eva.forecastr.clients.commandHandler.UserCommandHandler;
import de.eva.forecastr.clients.commandHandler.WalletCommandHandler;
import de.eva.forecastr.clients.formatter.ConsoleFormatter;
import de.eva.forecastr.core.interfaces.ForecastrGateway;
import de.eva.forecastr.core.interfaces.LoadTestRunner;
import de.eva.forecastr.core.models.User;
import de.eva.forecastr.core.models.exceptions.ClientException;
import java.io.InputStream;
import java.io.PrintStream;

public final class ConsoleClient {
  private final ForecastrGateway gateway;
  private final ClientSession session;
  private final ConsoleInput input;
  private final PrintStream output;
  private final UserCommandHandler userHandler;
  private final EventCommandHandler eventHandler;
  private final BetCommandHandler betHandler;
  private final WalletCommandHandler walletHandler;
  private final AdminCommandHandler adminHandler;

  public ConsoleClient(ForecastrGateway gateway) {
    this(gateway, System.in, System.out, LoadTestRunner.unavailable());
  }

  public ConsoleClient(ForecastrGateway gateway, LoadTestRunner loadTestRunner) {
    this(gateway, System.in, System.out, loadTestRunner);
  }

  public ConsoleClient(ForecastrGateway gateway, InputStream input, PrintStream output) {
    this(gateway, input, output, LoadTestRunner.unavailable());
  }

  public ConsoleClient(
      ForecastrGateway gateway,
      InputStream input,
      PrintStream output,
      LoadTestRunner loadTestRunner) {
    this.gateway = gateway;
    this.session = new ClientSession();
    this.input = new ConsoleInput(input, output);
    this.output = output;
    this.betHandler = new BetCommandHandler(gateway, session, this.input, output);
    this.userHandler = new UserCommandHandler(gateway, session, this.input, output);
    this.eventHandler = new EventCommandHandler(gateway, session, this.input, output, betHandler);
    this.walletHandler = new WalletCommandHandler(gateway, session, this.input, output);
    this.adminHandler = new AdminCommandHandler(gateway, loadTestRunner, this.input, output);
  }

  public void run() {
    output.println("FORECASTR");
    output.println("Prognosemärkte im Terminal");
    try {
      runSessions();
    } catch (ConsoleInput.EndOfInputException endOfInput) {
      // A closed input stream ends the interactive client quietly.
    } catch (ClientException exception) {
      output.println("Fehler: " + exception.getMessage());
    } catch (Exception exception) {
      output.println("Die Live-Verbindung konnte nicht sauber geschlossen werden.");
    }
  }

  private void runSessions() throws Exception {
    while (true) {
      User user = userHandler.chooseUser();
      if (user == null) {
        return;
      }
      session.user(user);
      gateway.selectUser(user.id());
      session.balance(gateway.balance(user.id()));
      try (AutoCloseable liveUpdates =
          gateway.liveUpdates(user.id(), update -> session.notify(ConsoleFormatter.live(update)))) {
        mainMenu();
      }
      if (session.user() != null) {
        return;
      }
    }
  }

  private void mainMenu() {
    boolean isRunning = true;
    while (isRunning) {
      flushNotifications();
      try {
        session.balance(gateway.balance(session.user().id()));
        printMainMenu();
        switch (input.askChoice("\nAuswahl > ")) {
          case "1" -> eventHandler.feed();
          case "2" -> eventHandler.search();
          case "3" -> betHandler.showBets();
          case "4" -> walletHandler.wallet();
          case "5" -> isRunning = userHandler.profile();
          case "6" -> openAdminPanel();
          case "9" -> {
            session.logout();
            isRunning = false;
          }
          case "0" -> isRunning = false;
          default -> output.println("Unbekannte Auswahl.");
        }
      } catch (ClientException exception) {
        output.println("Fehler: " + exception.getMessage());
      }
    }
  }

  private void printMainMenu() {
    ConsoleFormatter.section(output, "Hauptmenü");
    output.println("  Angemeldet als  " + session.user().username());
    output.println("  Guthaben        " + ConsoleFormatter.money(session.balance().balance()));
    output.println();
    output.println("  [1] Feed");
    output.println("  [2] Suchen");
    output.println("  [3] Meine Wetten");
    output.println("  [4] Wallet");
    output.println("  [5] Profil");
    if (session.user().isAdmin()) {
      output.println("  [6] Admin-Panel");
    }
    output.println("  [9] Ausloggen");
    output.println("  [0] Beenden");
  }

  private void openAdminPanel() {
    if (session.user().isAdmin()) {
      adminHandler.adminPanel();
    } else {
      output.println("Unbekannte Auswahl.");
    }
  }

  private void flushNotifications() {
    String message;
    while ((message = session.nextNotification()) != null) {
      output.println("\nHinweis: " + message);
    }
  }
}
