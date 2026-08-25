package de.eva.forecastr.clients.load.commandHandler;

import de.eva.forecastr.clients.load.StressScenarios;
import de.eva.forecastr.rest.CommunicationHandler;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public final class StressCommandHandler {
  private final StressScenarios scenarios;
  private final Scanner input;
  private final PrintStream output;
  private String lastFailure;

  public StressCommandHandler(CommunicationHandler communicationHandler) {
    this(communicationHandler, System.out);
  }

  public StressCommandHandler(CommunicationHandler communicationHandler, PrintStream output) {
    this(new StressScenarios(communicationHandler, output), new Scanner(System.in), output);
  }

  StressCommandHandler(StressScenarios scenarios, Scanner input) {
    this(scenarios, input, System.out);
  }

  StressCommandHandler(StressScenarios scenarios, Scanner input, PrintStream output) {
    this.scenarios = scenarios;
    this.input = input;
    this.output = output;
  }

  public boolean run(Path script) throws IOException {
    if (script != null) {
      return runScript(script);
    }
    return runInteractive();
  }

  public boolean execute(String line) {
    lastFailure = null;
    try {
      List<String> tokens = new ArrayList<>(List.of(line.trim().split("\\s+")));
      tokens.replaceAll(scenarios::expandToken);
      String command = tokens.getFirst().toLowerCase(Locale.ROOT);
      if (command.equals("help")) {
        printHelp();
        return true;
      }
      int requestCount = intOption(tokens, "-n", defaultCount(command));
      int concurrency = intOption(tokens, "-c", requestCount);
      Path reportPath = pathOption(tokens, "--out");
      boolean successful =
          scenarios.executeCommand(command, tokens, requestCount, concurrency, reportPath);
      if (!successful) {
        lastFailure = scenarios.getLastFailure();
      }
      return successful;
    } catch (Exception exception) {
      lastFailure = exception.getMessage();
      output.println("FAIL: " + exception.getMessage());
      return false;
    }
  }

  public String getLastFailure() {
    return lastFailure;
  }

  private boolean runScript(Path script) throws IOException {
    boolean successful = true;
    for (String rawLine : Files.readAllLines(script, StandardCharsets.UTF_8)) {
      String line = rawLine.strip();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      output.println("stress> " + line);
      successful &= execute(line);
    }
    return successful;
  }

  private boolean runInteractive() {
    boolean successful = true;
    output.println("Forecastr stress terminal. Type 'help' or 'quit'.");
    while (true) {
      output.print("stress> ");
      if (!input.hasNextLine()) {
        return successful;
      }
      String line = input.nextLine().trim();
      if (line.equals("quit") || line.equals("exit")) {
        return successful;
      }
      if (!line.isBlank()) {
        successful &= execute(line);
      }
    }
  }

  private int intOption(List<String> tokens, String option, int fallback) {
    int index = tokens.indexOf(option);
    return index >= 0 ? Integer.parseInt(tokens.get(index + 1)) : fallback;
  }

  private Path pathOption(List<String> tokens, String option) {
    int index = tokens.indexOf(option);
    return index >= 0 ? Path.of(tokens.get(index + 1)) : null;
  }

  private int defaultCount(String command) {
    return switch (command) {
      case "overbet" -> 500;
      case "resolve" -> 100;
      case "accounts" -> 2_000;
      case "stats" -> 5_000;
      case "sockets" -> 1_000;
      default -> 10_000;
    };
  }

  private void printHelp() {
    output.println(
        "Commands: seed <users> <events>; bets <eventId>; overbet <userId> <eventId>;"
            + " resolve <eventId>; search; feed; accounts; stats; mixed; rate <req/s>"
            + " <seconds>; sockets; threads. Burst commands accept -n, -c and --out."
            + " $user/$event refer to the latest seed.");
  }
}
