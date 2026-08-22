package de.eva.forecastr.rest.commandHandler;

import de.eva.forecastr.core.models.ImportReport;
import de.eva.forecastr.rest.CommunicationHandler;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class AdminRestHandler {
  private final CommunicationHandler communicationHandler;

  public AdminRestHandler(CommunicationHandler communicationHandler) {
    this.communicationHandler = communicationHandler;
  }

  public ImportReport importEvents(String path) {
    String query =
        path == null || path.isBlank()
            ? ""
            : "?path=" + URLEncoder.encode(path.trim(), StandardCharsets.UTF_8);
    return communicationHandler.value(
        communicationHandler.post("/admin/import" + query, Map.of()), ImportReport.class);
  }

}
