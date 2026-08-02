package de.eva.forecastr.clients;

import de.eva.forecastr.core.interfaces.ForecastrGateway;
import de.eva.forecastr.rest.CommunicationHandler;
import de.eva.forecastr.rest.RestForecastrGateway;
import java.util.List;

public final class ForecastrClient {
  private ForecastrClient() {}

  public static void main(String[] args) {
    List<String> arguments = List.of(args);
    String server = option(arguments, "--server", "http://localhost:8080");
    CommunicationHandler communicationHandler = new CommunicationHandler(server);
    ForecastrGateway gateway = new RestForecastrGateway(communicationHandler);
    try {
      new ConsoleClient(gateway).run();
    } catch (Exception exception) {
      System.err.println("Forecastr konnte nicht gestartet werden: " + exception.getMessage());
      System.exit(2);
    }
  }

  private static String option(List<String> arguments, String name, String fallback) {
    int index = arguments.indexOf(name);
    return index >= 0 && index + 1 < arguments.size() ? arguments.get(index + 1) : fallback;
  }

}
