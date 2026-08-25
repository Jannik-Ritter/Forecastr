package de.eva.forecastr.clients.commandHandler;

import de.eva.forecastr.clients.ConsoleInput;
import de.eva.forecastr.clients.formatter.ConsoleFormatter;
import de.eva.forecastr.core.interfaces.ForecastrGateway;
import de.eva.forecastr.core.interfaces.LoadTestRunner;
import de.eva.forecastr.core.models.AdminStats;
import de.eva.forecastr.core.models.ImportReport;
import de.eva.forecastr.core.models.LoadTestProgress;
import de.eva.forecastr.core.models.ManualResolutionResult;
import de.eva.forecastr.core.models.Market;
import de.eva.forecastr.core.models.TestEventsResponse;
import de.eva.forecastr.core.models.TestUsersResponse;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public final class AdminCommandHandler {
  private final ForecastrGateway gateway;
  private final LoadTestRunner loadTestRunner;
  private final ConsoleInput input;
  private final PrintStream output;

  public AdminCommandHandler(
      ForecastrGateway gateway,
      LoadTestRunner loadTestRunner,
      ConsoleInput input,
      PrintStream output) {
    this.gateway = gateway;
    this.loadTestRunner = loadTestRunner;
    this.input = input;
    this.output = output;
  }

  public void adminPanel() {
    while (true) {
      ConsoleFormatter.section(output, "Admin-Panel");
      output.println("  [1] Dashboard");
      output.println("  [2] Ereignis manuell auflösen");
      output.println("  [3] CSV importieren");
      output.println("  [4] Testdaten erzeugen");
      output.println("  [5] Lasttests");
      output.println("  [0] Hauptmenü");
      switch (input.askChoice("\nAuswahl > ")) {
        case "1" -> dashboard();
        case "2" -> resolveEvent();
        case "3" -> importEvents();
        case "4" -> seedData();
        case "5" -> loadTests();
        case "0" -> {
          return;
        }
        default -> output.println("Unbekannte Auswahl.");
      }
    }
  }

  private void dashboard() {
    AdminStats stats = gateway.adminStats();
    ConsoleFormatter.section(output, "Admin-Dashboard");
    output.printf("  Benutzer      %d%n", stats.users());
    output.printf("  Ereignisse    %d%n", stats.events());
    output.printf("  Wetten        %d%n", stats.bets());
    output.println("  Auszahlungen  " + ConsoleFormatter.money(stats.payouts()));
    output.println("  Anbieter-Guthaben (Gebühren)  " + ConsoleFormatter.money(stats.fees()));
    output.println();
    output.println("  Ereignisstatus");
    stats
        .eventsByStatus()
        .forEach((status, count) -> output.printf("    %-16s %d%n", status, count));
    output.println("  Resolver");
    stats.resolver().forEach((name, count) -> output.printf("    %-16s %d%n", name, count));
    waitForAdminMenu();
  }

  private void resolveEvent() {
    List<Market> markets = gateway.search("", "OPEN");
    ConsoleFormatter.section(output, "Offene Ereignisse auflösen");
    if (markets.isEmpty()) {
      output.println("Es gibt keine offenen Ereignisse.");
      waitForAdminMenu();
      return;
    }
    for (int index = 0; index < markets.size(); index++) {
      Market market = markets.get(index);
      output.printf(
          "[%d] %s (JA %s · NEIN %s)%n",
          index + 1,
          market.question(),
          ConsoleFormatter.money(market.yesPool()),
          ConsoleFormatter.money(market.noPool()));
    }
    Market market = selectMarket(markets);
    if (market == null) {
      return;
    }
    ConsoleFormatter.market(output, market, 1, 1, Instant.now());
    String outcome =
        resolutionOutcome(
            input.askChoice("\n[J] JA   [N] NEIN   [R] Rückerstatten   [X] Abbrechen > "));
    if (outcome == null) {
      return;
    }
    if (outcome.isEmpty()) {
      output.println("Unbekannte Auswahl.");
      return;
    }
    ManualResolutionResult result = gateway.resolve(market.id(), outcome);
    if (!result.changed()) {
      output.println("Das Ereignis war bereits abgeschlossen: " + result.status());
      return;
    }
    output.println(
        "Aufgelöst als "
            + result.status()
            + " · Auszahlungen "
            + ConsoleFormatter.money(result.payouts())
            + " · Gebühren "
            + ConsoleFormatter.money(result.fees()));
  }

  private void importEvents() {
    ConsoleFormatter.section(output, "CSV importieren");
    output.println("Ein leerer Pfad importiert die gebündelten Standarddateien.");
    output.println("Ein angegebener Pfad wird auf dem Server gelesen.");
    String path = input.ask("Serverpfad (leer = Standards) > ");
    ImportReport report = gateway.importEvents(path);
    output.printf(
        "Import abgeschlossen: %d übernommen, %d übersprungen, %d abgelehnt.%n",
        report.accepted(), report.skipped(), report.rejected());
    report.errors().forEach(error -> output.println("  - " + error));
  }

  private void seedData() {
    while (true) {
      ConsoleFormatter.section(output, "Testdaten erzeugen");
      output.println("  [1] Konten erstellen");
      output.println("  [2] Ereignisse erstellen");
      output.println("  [0] Admin-Panel");
      switch (input.askChoice("\nAuswahl > ")) {
        case "1" -> seedUsers();
        case "2" -> seedEvents();
        case "0" -> {
          return;
        }
        default -> output.println("Unbekannte Auswahl.");
      }
    }
  }

  private void seedUsers() {
    ConsoleFormatter.section(output, "Testkonten erstellen");
    Integer count = input.positiveInt(input.ask("Anzahl Konten > "));
    Integer betsPerUser = input.nonNegativeInt(input.ask("Wetten pro Konto > "));
    if (count == null || betsPerUser == null) {
      return;
    }
    PlannedTestBets plannedBets =
        betsPerUser == 0 ? new PlannedTestBets(null, null, null) : readPlannedBets();
    if (plannedBets == null) {
      return;
    }
    long totalBets = (long) count * betsPerUser;
    output.printf("Es werden %d Konten mit insgesamt %d Wetten erzeugt.%n", count, totalBets);
    TestUsersResponse response =
        gateway.seedTestUsers(
            count, betsPerUser, plannedBets.eventId(), plannedBets.outcome(), plannedBets.stake());
    output.printf(
        "%d Konten und %d Wetten erzeugt.%n", response.userIds().size(), response.betIds().size());
  }

  private PlannedTestBets readPlannedBets() {
    Long eventId = null;
    String eventValue = input.ask("Ereignis-ID (leer = zufällig) > ");
    if (!eventValue.isEmpty()) {
      eventId = input.positiveLong(eventValue);
      if (eventId == null) {
        return null;
      }
    }
    String outcome = null;
    String outcomeValue = input.ask("Ergebnis [JA/NEIN] (leer = zufällig) > ");
    if (!outcomeValue.isEmpty()) {
      outcome = input.outcome(outcomeValue);
      if (outcome == null) {
        return null;
      }
    }
    BigDecimal stake = null;
    String stakeValue = input.ask("Einsatz in EUR (leer = zufällig 1,00–100,00 €) > ");
    if (!stakeValue.isEmpty()) {
      stake = input.amount(stakeValue);
      if (stake == null) {
        return null;
      }
    }
    return new PlannedTestBets(eventId, outcome, stake);
  }

  private void seedEvents() {
    ConsoleFormatter.section(output, "Testereignisse erstellen");
    Integer count = input.positiveInt(input.ask("Anzahl Ereignisse > "));
    if (count == null) {
      return;
    }
    String lifetimeValue = input.ask("Laufzeit in Minuten (leer = 10) > ");
    Integer lifetime = null;
    if (!lifetimeValue.isEmpty()) {
      lifetime = input.boundedPositiveInt(lifetimeValue, 1440);
      if (lifetime == null) {
        return;
      }
    }
    output.printf(
        "Es werden %d Ereignisse mit %d Minuten Laufzeit erzeugt.%n",
        count, lifetime == null ? 10 : lifetime);
    TestEventsResponse response = gateway.seedTestEvents(count, lifetime);
    output.printf("%d Ereignisse erzeugt.%n", response.eventIds().size());
  }

  private void loadTests() {
    while (true) {
      ConsoleFormatter.section(output, "Lasttests");
      output.println("  [1] Schnelltest (begrenzte Last)");
      output.println("  [2] Vollständige NFR-Simulation");
      output.println("  [0] Admin-Panel");
      switch (input.askChoice("\nAuswahl > ")) {
        case "1" -> runLoadTest("Schnelltest", loadTestRunner::runQuick);
        case "2" -> runFullLoadTest();
        case "0" -> {
          return;
        }
        default -> output.println("Unbekannte Auswahl.");
      }
    }
  }

  private void runFullLoadTest() {
    output.println(
        "Die vollständige Suite benötigt das Serverprofil 'test' und kann mehrere Minuten dauern.");
    runLoadTest("NFR-Simulation", loadTestRunner::runFull);
  }

  private void runLoadTest(
      String title, Function<Consumer<LoadTestProgress>, Boolean> loadTest) {
    List<LoadTestProgress> updates = new ArrayList<>();
    long startedAt = System.nanoTime();
    output.println(title + " läuft …");
    boolean successful =
        loadTest.apply(
            progress -> {
              updates.add(progress);
              renderLoadTestProgress(progress);
            });
    if (!updates.isEmpty()) {
      output.println();
    }
    double seconds = (System.nanoTime() - startedAt) / 1_000_000_000d;
    LoadTestProgress latest = updates.isEmpty() ? null : updates.getLast();
    if (latest == null) {
      output.println(title + (successful ? " bestanden." : " fehlgeschlagen."));
    } else {
      output.printf(
          Locale.GERMANY,
          "%s %s (%d/%d Prüfungen, %.1f s).%n",
          title,
          successful ? "bestanden" : "fehlgeschlagen",
          latest.completed(),
          latest.total(),
          seconds);
    }
    printFailedLoadTestSteps(updates);
  }

  private void renderLoadTestProgress(LoadTestProgress progress) {
    String status =
        switch (progress.status()) {
          case RUNNING -> "läuft";
          case PASSED -> "OK";
          case FAILED -> "FEHLER";
        };
    output.printf(
        "\r  %s  %2d/%-2d  %-34.34s %6s",
        ConsoleFormatter.progress(progress.completed(), progress.total()),
        progress.completed(),
        progress.total(),
        progress.step(),
        status);
    output.flush();
  }

  private void printFailedLoadTestSteps(List<LoadTestProgress> updates) {
    boolean hasFailures = false;
    for (LoadTestProgress progress : updates) {
      if (progress.status() == LoadTestProgress.Status.FAILED) {
        if (!hasFailures) {
          output.println("Fehlgeschlagen:");
          hasFailures = true;
        }
        output.println("  - " + progress.step() + failureSuffix(progress.failureDetail()));
      }
    }
  }

  private String failureSuffix(String detail) {
    if (detail == null || detail.isBlank()) {
      return "";
    }
    String compact = detail.replaceAll("\\s+", " ").trim();
    int maximumLength = 160;
    if (compact.length() > maximumLength) {
      compact = compact.substring(0, maximumLength - 1) + "…";
    }
    return " – " + compact;
  }

  private Market selectMarket(List<Market> markets) {
    String choice = input.askChoice("\nNummer auswählen   [X] Admin-Panel > ");
    if (choice.equalsIgnoreCase("x")) {
      return null;
    }
    try {
      return markets.get(Integer.parseInt(choice) - 1);
    } catch (NumberFormatException | IndexOutOfBoundsException exception) {
      output.println("Ungültige Auswahl.");
      return null;
    }
  }

  private String resolutionOutcome(String action) {
    return switch (action.toLowerCase(Locale.ROOT)) {
      case "j" -> "YES";
      case "n" -> "NO";
      case "r" -> "REFUND";
      case "x" -> null;
      default -> "";
    };
  }

  private void waitForAdminMenu() {
    while (!input.askChoice("\n[X] Admin-Panel > ").equalsIgnoreCase("x")) {
      output.println("Bitte gib X ein, um zum Admin-Panel zurückzukehren.");
    }
  }

  private record PlannedTestBets(Long eventId, String outcome, BigDecimal stake) {}
}
