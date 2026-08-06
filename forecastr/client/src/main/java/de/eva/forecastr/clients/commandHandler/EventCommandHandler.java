package de.eva.forecastr.clients.commandHandler;

import de.eva.forecastr.clients.ClientSession;
import de.eva.forecastr.clients.ConsoleInput;
import de.eva.forecastr.clients.formatter.ConsoleFormatter;
import de.eva.forecastr.core.interfaces.ForecastrGateway;
import de.eva.forecastr.core.models.Market;
import java.io.PrintStream;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public final class EventCommandHandler {
  private final ForecastrGateway gateway;
  private final ClientSession session;
  private final ConsoleInput input;
  private final PrintStream output;

  public EventCommandHandler(
      ForecastrGateway gateway,
      ClientSession session,
      ConsoleInput input,
      PrintStream output) {
    this.gateway = gateway;
    this.session = session;
    this.input = input;
    this.output = output;
  }

  public void feed() {
    List<Market> markets = gateway.feed();
    int index = 0;
    while (true) {
      if (markets.isEmpty()) {
        ConsoleFormatter.section(output, "Feed");
        output.println("Im Moment gibt es keine offenen Märkte.");
        if (input.askChoice("\n[R] Aktualisieren   [X] Hauptmenü > ").equalsIgnoreCase("r")) {
          markets = gateway.feed();
          index = 0;
          continue;
        }
        return;
      }
      index = Math.max(0, Math.min(index, markets.size() - 1));
      Market market = markets.get(index);
      session.remember(market);
      ConsoleFormatter.market(output, market, index + 1, markets.size(), Instant.now());
      String action =
          input.askChoice(
              "\n[W] Weiter  [Z] Zurück  [R] Aktualisieren  [X] Menü > ");
      switch (action.toLowerCase(Locale.ROOT)) {
        case "w" -> {
          if (index + 1 < markets.size()) {
            index++;
          } else {
            output.println("Du hast das Ende des Feeds erreicht.");
          }
        }
        case "z" -> index = Math.max(0, index - 1);
        case "r" -> {
          long selectedEventId = market.id();
          markets = gateway.feed();
          index = findMarket(markets, selectedEventId);
        }
        case "x" -> {
          return;
        }
        default -> output.println("Unbekannte Auswahl.");
      }
    }
  }

  public void search() {
    ConsoleFormatter.section(output, "Suchen");
    String text = input.ask("Suchbegriff (leer = alle) > ");
    List<Market> results = gateway.search(text, "");
    if (results.isEmpty()) {
      output.println("Keine passenden Märkte gefunden.");
      waitForMainMenu();
      return;
    }
    ConsoleFormatter.section(output, "Suchergebnisse");
    for (int index = 0; index < results.size(); index++) {
      Market market = results.get(index);
      session.remember(market);
      output.printf(
          "  [%d] %s%n      %s%n",
          index + 1, market.question(), ConsoleFormatter.eventStatus(market.status()));
    }
    openSearchResult(results);
  }

  private void openSearchResult(List<Market> results) {
    String choice = input.askChoice("\nNummer öffnen   [X] Hauptmenü > ");
    if (choice.equalsIgnoreCase("x")) {
      return;
    }
    try {
      Market market = results.get(Integer.parseInt(choice) - 1);
      ConsoleFormatter.market(output, market, 1, 1, Instant.now());
    } catch (IndexOutOfBoundsException | NumberFormatException exception) {
      output.println("Ungültige Auswahl.");
    }
  }

  private int findMarket(List<Market> markets, long eventId) {
    for (int index = 0; index < markets.size(); index++) {
      if (markets.get(index).id() == eventId) {
        return index;
      }
    }
    return 0;
  }

  private void waitForMainMenu() {
    while (!input.askChoice("\n[X] Hauptmenü > ").equalsIgnoreCase("x")) {
      output.println("Bitte gib X ein, um zum Hauptmenü zurückzukehren.");
    }
  }
}
