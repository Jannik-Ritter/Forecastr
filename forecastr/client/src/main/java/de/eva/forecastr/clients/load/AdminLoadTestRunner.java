package de.eva.forecastr.clients.load;

import de.eva.forecastr.clients.load.commandHandler.StressCommandHandler;
import de.eva.forecastr.core.interfaces.LoadTestRunner;
import de.eva.forecastr.core.models.LoadTestProgress;
import de.eva.forecastr.rest.CommunicationHandler;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Consumer;

public final class AdminLoadTestRunner implements LoadTestRunner {
  private static final PrintStream QUIET_OUTPUT = new PrintStream(OutputStream.nullOutputStream());
  private static final List<LoadTestStep> QUICK_STEPS =
      List.of(
          new LoadTestStep("Testdaten erzeugen", "seed 100 2"),
          new LoadTestStep("Gleichzeitige Wetten", "bets $event -n 100 -c 20"),
          new LoadTestStep("Parallele Suche", "search -n 500 -c 50"),
          new LoadTestStep("Paralleler Feed", "feed -n 500 -c 50"),
          new LoadTestStep("Statistik unter Last", "stats -n 100 -c 20"),
          new LoadTestStep("Server-Worker", "threads"));

  private final CommunicationHandler communicationHandler;
  private final StressCommandHandler stressCommandHandler;

  public AdminLoadTestRunner(CommunicationHandler communicationHandler) {
    this.communicationHandler = communicationHandler;
    this.stressCommandHandler = new StressCommandHandler(communicationHandler);
  }

  @Override
  public boolean runQuick() {
    return runQuick(stressCommandHandler, progress -> {});
  }

  @Override
  public boolean runQuick(Consumer<LoadTestProgress> progress) {
    StressCommandHandler quietHandler =
        new StressCommandHandler(communicationHandler, QUIET_OUTPUT);
    return runQuick(quietHandler, progress);
  }

  @Override
  public boolean runFull() {
    return new LoadSimulation(communicationHandler).run();
  }

  @Override
  public boolean runFull(Consumer<LoadTestProgress> progress) {
    return new LoadSimulation(communicationHandler, QUIET_OUTPUT, progress).run();
  }

  @Override
  public boolean execute(String command) {
    return stressCommandHandler.execute(command);
  }

  private boolean runQuick(
      StressCommandHandler commandHandler, Consumer<LoadTestProgress> progress) {
    boolean successful = true;
    int completed = 0;
    for (LoadTestStep step : QUICK_STEPS) {
      progress.accept(LoadTestProgress.running(completed, QUICK_STEPS.size(), step.name()));
      boolean stepSuccessful = commandHandler.execute(step.command());
      successful &= stepSuccessful;
      completed++;
      progress.accept(
          LoadTestProgress.completed(
              completed,
              QUICK_STEPS.size(),
              step.name(),
              stepSuccessful,
              commandHandler.getLastFailure()));
    }
    return successful;
  }

  private record LoadTestStep(String name, String command) {}
}
