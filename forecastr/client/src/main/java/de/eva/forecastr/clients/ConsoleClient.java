package de.eva.forecastr.clients;

import de.eva.forecastr.clients.commandHandler.EventCommandHandler;
import de.eva.forecastr.clients.commandHandler.UserCommandHandler;
import de.eva.forecastr.clients.commandHandler.WalletCommandHandler;
import de.eva.forecastr.clients.formatter.ConsoleFormatter;
import de.eva.forecastr.core.interfaces.ForecastrGateway;
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
  private final WalletCommandHandler walletHandler;

  public ConsoleClient(ForecastrGateway gateway) {
    this(gateway, System.in, System.out);
  }

  public ConsoleClient(ForecastrGateway gateway, InputStream input, PrintStream output) {
    this.gateway = gateway;
    this.session = new ClientSession();
    this.input = new ConsoleInput(input, output);
    this.output = output;
    this.userHandler = new UserCommandHandler(gateway, session, this.input, output);
    this.eventHandler = new EventCommandHandler(gateway, session, this.input, output);
    this.walletHandler = new WalletCommandHandler(gateway, session, this.input, output);
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
    }
  }

  private void runSessions() {
    while (true) {
      User user = userHandler.chooseUser();
      if (user == null) {
        return;
      }
      session.user(user);
      gateway.selectUser(user.id());
      session.balance(gateway.balance(user.id()));
      mainMenu();
      if (session.user() != null) {
        return;
      }
    }
  }

  private void mainMenu() {
    boolean isRunning = true;
    while (isRunning) {
      try {
        session.balance(gateway.balance(session.user().id()));
        ConsoleFormatter.section(output, "Hauptmenü");
        output.println("  Angemeldet als  " + session.user().username());
        output.println("  Guthaben        " + ConsoleFormatter.money(session.balance().balance()));
        output.println("  [1] Feed");
        output.println("  [2] Suchen");
        output.println("  [3] Wallet");
        output.println("  [4] Profil");
        output.println("  [9] Ausloggen");
        output.println("  [0] Beenden");
        switch (input.askChoice("\nAuswahl > ")) {
          case "1" -> eventHandler.feed();
          case "2" -> eventHandler.search();
          case "3" -> walletHandler.wallet();
          case "4" -> isRunning = userHandler.profile();
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
}
