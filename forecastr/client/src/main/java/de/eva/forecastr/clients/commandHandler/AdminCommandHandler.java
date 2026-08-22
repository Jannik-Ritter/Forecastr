package de.eva.forecastr.clients.commandHandler;

import de.eva.forecastr.clients.ConsoleInput;
import de.eva.forecastr.clients.formatter.ConsoleFormatter;
import de.eva.forecastr.core.interfaces.ForecastrGateway;
import de.eva.forecastr.core.models.ImportReport;
import java.io.PrintStream;

public final class AdminCommandHandler {
  private final ForecastrGateway gateway;
  private final ConsoleInput input;
  private final PrintStream output;

  public AdminCommandHandler(
      ForecastrGateway gateway, ConsoleInput input, PrintStream output) {
    this.gateway = gateway;
    this.input = input;
    this.output = output;
  }

  public void adminPanel() {
    while (true) {
      ConsoleFormatter.section(output, "Admin-Panel");
      output.println("  [1] CSV importieren");
      output.println("  [0] Hauptmenü");
      switch (input.askChoice("\nAuswahl > ")) {
        case "1" -> importEvents();
        case "0" -> {
          return;
        }
        default -> output.println("Unbekannte Auswahl.");
      }
    }
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
}
