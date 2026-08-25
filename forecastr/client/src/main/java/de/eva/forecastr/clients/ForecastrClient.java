package de.eva.forecastr.clients;

import de.eva.forecastr.clients.load.AdminLoadTestRunner;
import de.eva.forecastr.clients.load.LoadSimulation;
import de.eva.forecastr.clients.load.commandHandler.StressCommandHandler;
import de.eva.forecastr.core.interfaces.ForecastrGateway;
import de.eva.forecastr.core.models.User;
import de.eva.forecastr.rest.CommunicationHandler;
import de.eva.forecastr.rest.RestForecastrGateway;
import de.eva.forecastr.websocket.WebSocketClient;
import java.nio.file.Path;
import java.util.List;

public final class ForecastrClient {
  private ForecastrClient() {}

  public static void main(String[] args) {
    List<String> arguments = List.of(args);
    String server = option(arguments, "--server", "http://localhost:8080");
    CommunicationHandler communicationHandler = new CommunicationHandler(server);
    ForecastrGateway gateway =
        new RestForecastrGateway(communicationHandler, new WebSocketClient(communicationHandler));
    try {
      if (arguments.contains("--simulate")) {
        selectAdmin(gateway);
        exitIfFailed(new LoadSimulation(communicationHandler).run());
        return;
      }
      if (arguments.contains("--stress")) {
        selectAdmin(gateway);
        String script = option(arguments, "--script", null);
        boolean successful =
            new StressCommandHandler(communicationHandler)
                .run(script == null ? null : Path.of(script));
        exitIfFailed(successful);
        return;
      }
      new ConsoleClient(gateway, new AdminLoadTestRunner(communicationHandler)).run();
    } catch (Exception exception) {
      System.err.println("Forecastr konnte nicht gestartet werden: " + exception.getMessage());
      System.exit(2);
    }
  }

  private static String option(List<String> arguments, String name, String fallback) {
    int index = arguments.indexOf(name);
    return index >= 0 && index + 1 < arguments.size() ? arguments.get(index + 1) : fallback;
  }

  private static void selectAdmin(ForecastrGateway gateway) {
    for (User user : gateway.users()) {
      if (user.isAdmin() && user.deletedAt() == null) {
        gateway.selectUser(user.id());
        return;
      }
    }
    throw new IllegalStateException("Kein aktives Administratorkonto vorhanden.");
  }

  private static void exitIfFailed(boolean successful) {
    if (!successful) {
      System.exit(1);
    }
  }
}
