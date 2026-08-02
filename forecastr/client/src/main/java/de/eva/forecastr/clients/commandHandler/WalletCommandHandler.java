package de.eva.forecastr.clients.commandHandler;

import de.eva.forecastr.clients.ClientSession;
import de.eva.forecastr.clients.ConsoleInput;
import de.eva.forecastr.clients.formatter.ConsoleFormatter;
import de.eva.forecastr.core.interfaces.ForecastrGateway;
import java.io.PrintStream;
import java.math.BigDecimal;

public final class WalletCommandHandler {
  private final ForecastrGateway gateway;
  private final ClientSession session;
  private final ConsoleInput input;
  private final PrintStream output;

  public WalletCommandHandler(
      ForecastrGateway gateway, ClientSession session, ConsoleInput input, PrintStream output) {
    this.gateway = gateway;
    this.session = session;
    this.input = input;
    this.output = output;
  }

  public void wallet() {
    while (true) {
      session.balance(gateway.balance(session.user().id()));
      ConsoleFormatter.section(output, "Wallet");
      output.println(
          "  Verfügbares Guthaben  " + ConsoleFormatter.money(session.balance().balance()));
      String action = input.askChoice("\n[E] Einzahlen   [A] Auszahlen   [X] Hauptmenü > ");
      if (action.equalsIgnoreCase("x")) {
        return;
      }
      if (!action.equalsIgnoreCase("e") && !action.equalsIgnoreCase("a")) {
        output.println("Unbekannte Auswahl.");
        continue;
      }
      BigDecimal amount = input.amount(input.ask("Betrag in EUR > "));
      if (amount == null) {
        continue;
      }
      session.balance(
          action.equalsIgnoreCase("e")
              ? gateway.deposit(session.user().id(), amount)
              : gateway.withdraw(session.user().id(), amount));
      output.println("Neues Guthaben: " + ConsoleFormatter.money(session.balance().balance()));
    }
  }
}
