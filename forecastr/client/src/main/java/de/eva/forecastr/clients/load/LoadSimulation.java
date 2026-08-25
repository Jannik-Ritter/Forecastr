package de.eva.forecastr.clients.load;

import com.fasterxml.jackson.databind.JsonNode;
import de.eva.forecastr.clients.load.commandHandler.StressCommandHandler;
import de.eva.forecastr.core.models.LoadTestProgress;
import de.eva.forecastr.rest.CommunicationHandler;
import de.eva.forecastr.rest.RestResponse;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public final class LoadSimulation {
  private static final int CHECK_COUNT = 15;

  private final CommunicationHandler communicationHandler;
  private final long clientStarted = System.nanoTime();
  private final List<String> results = new ArrayList<>();
  private final PrintStream output;
  private final Consumer<LoadTestProgress> progress;
  private int completedChecks;
  private String failureDetail;

  public LoadSimulation(CommunicationHandler communicationHandler) {
    this(communicationHandler, System.out, loadTestProgress -> {});
  }

  LoadSimulation(
      CommunicationHandler communicationHandler,
      PrintStream output,
      Consumer<LoadTestProgress> progress) {
    this.communicationHandler = communicationHandler;
    this.output = output;
    this.progress = progress;
  }

  public boolean run() {
    results.clear();
    completedChecks = 0;
    output.println("Forecastr automated NFR simulation");
    boolean isSuccessful = true;
    isSuccessful &= check("startup readiness <15s", "Server-Bereitschaft", this::readiness);
    StressCommandHandler stressCommandHandler =
        new StressCommandHandler(communicationHandler, output);
    isSuccessful &=
        check(
            "seed load fixtures",
            "Lasttest-Daten erzeugen",
            () -> execute(stressCommandHandler, "seed 10000 4"));
    isSuccessful &=
        check(
            "bet atomicity: 1000 simultaneous",
            "Atomare Wetten",
            () ->
                execute(
                    stressCommandHandler,
                    "bets $event -n 1000 -c 1000 --out simulation-report.csv"));
    isSuccessful &=
        check(
            "over-balance atomicity",
            "Kontostand-Schutz",
            () ->
                execute(
                    stressCommandHandler,
                    "overbet $user $event -n 500 -c 500 --out" + " simulation-report.csv"));
    isSuccessful &=
        check(
            "resolution atomicity and exact payout math",
            "Auflösung und Auszahlung",
            () ->
                execute(
                    stressCommandHandler,
                    "resolve $event -n 100 -c 100 --out" + " simulation-report.csv"));
    isSuccessful &=
        check(
            "CSV lifetime and question validation", "CSV-Validierung", this::importValidation);
    isSuccessful &=
        check("resolver caution and expiry", "Resolver und Erstattung", this::resolverCaution);
    isSuccessful &=
        check(
            "parallel search",
            "Parallele Suche",
            () ->
                execute(
                    stressCommandHandler,
                    "search -n 10000 -c 10000 --out simulation-report.csv"));
    isSuccessful &=
        check(
            "parallel feed",
            "Paralleler Feed",
            () ->
                execute(
                    stressCommandHandler,
                    "feed -n 10000 -c 10000 --out simulation-report.csv"));
    isSuccessful &=
        check(
            "account cycles",
            "Parallele Kontovorgänge",
            () ->
                execute(
                    stressCommandHandler,
                    "accounts -n 2000 -c 2000 --out simulation-report.csv"));
    isSuccessful &=
        check(
            "stats during betting",
            "Statistik unter Last",
            () ->
                execute(
                    stressCommandHandler,
                    "stats -n 5000 -c 5000 --out simulation-report.csv"));
    isSuccessful &=
        check(
            "realistic mixed load",
            "Gemischte Last",
            () ->
                execute(
                    stressCommandHandler,
                    "mixed -n 10000 -c 10000 --out simulation-report.csv"));
    isSuccessful &=
        check(
            "50 req/s for 60 seconds",
            "Dauerlast (50 Anfragen/s)",
            () -> {
              Thread.sleep(5_000);
              return execute(
                  stressCommandHandler, "rate 50 60 --out simulation-report.csv");
            });
    isSuccessful &=
        check(
            "WebSocket push latency",
            "WebSocket-Latenz",
            () -> execute(stressCommandHandler, "sockets -n 1000"));
    isSuccessful &=
        check(
            "multiple server workers",
            "Server-Worker",
            () -> execute(stressCommandHandler, "threads"));
    output.println("\nNFR REPORT");
    results.forEach(output::println);
    output.println(isSuccessful ? "OVERALL PASS" : "OVERALL FAIL");
    return isSuccessful;
  }

  private boolean readiness() {
    long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
    while (System.nanoTime() < deadline) {
      if (communicationHandler.get("/stats").isSuccess()) {
        long measured = System.nanoTime() - clientStarted;
        String supplied = System.getenv("FORECASTR_SERVER_STARTED_AT_MS");
        if (supplied != null) {
          long actual = System.currentTimeMillis() - Long.parseLong(supplied);
          output.println("Server ready after " + actual + " ms from supplied process start");
          return actual < 15000;
        }
        output.printf(
            Locale.ROOT, "Server answered %.0f ms after client polling began%n", measured / 1e6);
        return measured < Duration.ofSeconds(15).toNanos();
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }

  private boolean importValidation() {
    JsonNode report = importFile(fixturePath("nfr-import.csv"));
    int rejected = report.path("rejected").asInt(),
        accepted = report.path("accepted").asInt(),
        skipped = report.path("skipped").asInt();
    output.printf("Import accepted=%d skipped=%d rejected=%d%n", accepted, skipped, rejected);
    return rejected == 2 && accepted + skipped == 2;
  }

  private boolean resolverCaution() {
    JsonNode report = importFile(fixturePath("nfr-resolver.csv"));
    if (report.path("rejected").asInt() != 0) {
      return false;
    }
    communicationHandler.post("/admin/events/9200001/resolve", Map.of("outcome", "YES"));
    communicationHandler.post("/admin/events/9200002/resolve", Map.of("outcome", "NO"));
    String yes = status(9200001), no = status(9200002), uncertain = status(9200003);
    if (!yes.equals("RESOLVED_YES") || !no.equals("RESOLVED_NO") || !uncertain.equals("OPEN"))
      return false;
    long deadline = System.nanoTime() + Duration.ofSeconds(75).toNanos();
    while (System.nanoTime() < deadline) {
      if (status(9200003).equals("EXPIRED")) {
        return true;
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return false;
  }

  private JsonNode importFile(Path path) {
    String encoded = URLEncoder.encode(path.toString(), StandardCharsets.UTF_8);
    RestResponse response = communicationHandler.post("/admin/import?path=" + encoded, Map.of());
    if (!response.isSuccess()) {
      throw new IllegalStateException("Import failed: " + response.body());
    }
    return communicationHandler.tree(response);
  }

  private String status(long eventId) {
    RestResponse response = communicationHandler.get("/events/" + eventId);
    return response.isSuccess()
        ? communicationHandler.tree(response).path("status").asText()
        : "HTTP_" + response.status();
  }

  private boolean execute(StressCommandHandler commandHandler, String command) {
    boolean successful = commandHandler.execute(command);
    if (!successful) {
      failureDetail = commandHandler.getLastFailure();
    }
    return successful;
  }

  private Path fixturePath(String fileName) {
    Path workingDirectory = Path.of("").toAbsolutePath();
    List<Path> candidates =
        List.of(
            workingDirectory.resolve(fileName),
            workingDirectory.resolve("forecastr").resolve(fileName));
    for (Path candidate : candidates) {
      if (Files.isRegularFile(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException("Testdatei nicht gefunden: " + fileName);
  }

  private boolean check(String name, String displayName, Check check) {
    failureDetail = null;
    progress.accept(LoadTestProgress.running(completedChecks, CHECK_COUNT, displayName));
    output.println("\n== " + name + " ==");
    boolean pass;
    try {
      pass = check.run();
    } catch (Exception exception) {
      pass = false;
      failureDetail = exception.getMessage();
      output.println("Error: " + exception.getMessage());
    }
    results.add((pass ? "PASS " : "FAIL ") + name);
    completedChecks++;
    progress.accept(
        LoadTestProgress.completed(
            completedChecks, CHECK_COUNT, displayName, pass, failureDetail));
    return pass;
  }

  @FunctionalInterface
  private interface Check {
    boolean run() throws Exception;
  }
}
