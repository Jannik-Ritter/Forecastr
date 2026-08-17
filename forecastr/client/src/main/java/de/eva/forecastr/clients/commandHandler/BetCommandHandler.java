package de.eva.forecastr.clients.commandHandler;

import de.eva.forecastr.clients.ClientSession;
import de.eva.forecastr.clients.ConsoleInput;
import de.eva.forecastr.clients.formatter.ConsoleFormatter;
import de.eva.forecastr.core.interfaces.ForecastrGateway;
import de.eva.forecastr.core.models.Bet;
import de.eva.forecastr.core.models.Market;
import de.eva.forecastr.core.models.exceptions.ClientException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public final class BetCommandHandler {
  private static final BigDecimal FEE_RATE = new BigDecimal("0.05");

  private final ForecastrGateway gateway;
  private final ClientSession session;
  private final ConsoleInput input;
  private final PrintStream output;

  public BetCommandHandler(
      ForecastrGateway gateway, ClientSession session, ConsoleInput input, PrintStream output) {
    this.gateway = gateway;
    this.session = session;
    this.input = input;
    this.output = output;
  }

  public void showBets() {
    while (true) {
      List<Bet> bets = ConsoleFormatter.groupBets(gateway.bets(session.user().id()));
      ConsoleFormatter.section(output, "Meine Wetten");
      if (bets.isEmpty()) {
        output.println("Du hast noch keine Wetten platziert.");
      } else {
        printBets(bets);
      }
      String action = input.askChoice("\n[R] Aktualisieren   [X] Hauptmenü > ");
      if (action.equalsIgnoreCase("x")) {
        return;
      }
      if (!action.equalsIgnoreCase("r")) {
        output.println("Unbekannte Auswahl.");
      }
    }
  }

  public boolean placeBet(Market market, String outcome) {
    if (!"OPEN".equals(market.status())) {
      output.println("Dieser Markt ist nicht mehr für Wetten geöffnet.");
      return false;
    }
    output.println(
        "\nWette auf "
            + ConsoleFormatter.outcome(outcome)
            + " · Verfügbar "
            + ConsoleFormatter.money(session.balance().balance()));
    while (true) {
      String value = input.ask("Einsatz in EUR, oder X zum Abbrechen > ");
      if (value.equalsIgnoreCase("x")) {
        return false;
      }
      BigDecimal stake = input.amount(value);
      if (stake == null) {
        continue;
      }
      if (stake.compareTo(session.balance().balance()) > 0) {
        output.println(
            "Das Guthaben reicht für diesen Einsatz nicht aus. Verfügbar: "
                + ConsoleFormatter.money(session.balance().balance()));
        continue;
      }
      String confirmation =
          input.askChoice(
              ConsoleFormatter.money(stake)
                  + " auf "
                  + ConsoleFormatter.outcome(outcome)
                  + " setzen? Möglicher Gewinn: "
                  + ConsoleFormatter.money(potentialProfit(market, outcome, stake))
                  + " [J/N] > ");
      if (!confirmation.equalsIgnoreCase("j")) {
        output.println("Wette abgebrochen.");
        return false;
      }
      try {
        gateway.placeBet(session.user().id(), market.id(), outcome, stake);
      } catch (ClientException exception) {
        if (!exception.getMessage().contains("Guthaben reicht")) {
          throw exception;
        }
        session.balance(gateway.balance(session.user().id()));
        output.println("Fehler: " + exception.getMessage());
        continue;
      }
      session.balance(gateway.balance(session.user().id()));
      session.remember(gateway.event(market.id()));
      output.println(
          "Wette erfolgreich platziert. Neues Guthaben: "
              + ConsoleFormatter.money(session.balance().balance()));
      return true;
    }
  }

  private void printBets(List<Bet> bets) {
    String previousStatus = null;
    for (Bet bet : bets) {
      String status = ConsoleFormatter.betStatus(bet.status());
      if (!status.equals(previousStatus)) {
        output.println("\n" + status.toUpperCase(Locale.GERMANY));
        previousStatus = status;
      }
      printBet(bet);
    }
  }

  private void printBet(Bet bet) {
    Market market = cachedEvent(bet.eventId());
    output.println("  " + (market == null ? "Markt " + bet.eventId() : market.question()));
    output.println(
        "    Tipp "
            + ConsoleFormatter.outcome(bet.outcome())
            + " · Einsatz "
            + ConsoleFormatter.money(bet.stake())
            + " · platziert "
            + ConsoleFormatter.date(bet.placedAt()));
    switch (bet.status() == null ? "" : bet.status()) {
      case "OPEN" -> printOpenBet(market);
      case "WON" ->
          output.println(
              "    Ausgezahlt "
                  + ConsoleFormatter.money(bet.payoutAmount())
                  + " · Gebühr "
                  + ConsoleFormatter.money(bet.feeAmount()));
      case "LOST" -> output.println("    Auszahlung " + ConsoleFormatter.money(BigDecimal.ZERO));
      case "REFUNDED" ->
          output.println("    Erstattet " + ConsoleFormatter.money(bet.payoutAmount()));
      default -> output.println("    Auszahlung automatisch nach der Entscheidung");
    }
  }

  private void printOpenBet(Market market) {
    if (market == null || market.settlementAt() == null) {
      output.println("    Auszahlung automatisch nach der Entscheidung");
      return;
    }
    output.println(
        "    Abrechnung geplant "
            + ConsoleFormatter.date(market.settlementAt())
            + " ("
            + ConsoleFormatter.deadline(market.settlementAt(), Instant.now())
            + ")");
  }

  private Market cachedEvent(long eventId) {
    Market cached = session.event(eventId);
    if (cached != null) {
      return cached;
    }
    try {
      Market event = gateway.event(eventId);
      session.remember(event);
      return event;
    } catch (ClientException unavailableEvent) {
      return null;
    }
  }

  private BigDecimal potentialProfit(Market market, String outcome, BigDecimal stake) {
    BigDecimal selectedPool = "YES".equals(outcome) ? market.yesPool() : market.noPool();
    BigDecimal otherPool = "YES".equals(outcome) ? market.noPool() : market.yesPool();
    BigDecimal winningPool = selectedPool.add(stake);
    BigDecimal totalPool = winningPool.add(otherPool);
    BigDecimal gross = stake.multiply(totalPool).divide(winningPool, 2, RoundingMode.HALF_EVEN);
    BigDecimal grossProfit = gross.subtract(stake).max(BigDecimal.ZERO);
    BigDecimal fee = grossProfit.multiply(FEE_RATE).setScale(2, RoundingMode.HALF_EVEN);
    return grossProfit.subtract(fee).setScale(2, RoundingMode.HALF_EVEN);
  }
}
