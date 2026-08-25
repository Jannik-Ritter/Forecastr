package de.eva.forecastr.rest.commandHandler;

import de.eva.forecastr.core.models.AdminStats;
import de.eva.forecastr.core.models.ImportReport;
import de.eva.forecastr.core.models.ManualResolutionResult;
import de.eva.forecastr.core.models.TestEventsResponse;
import de.eva.forecastr.core.models.TestUsersResponse;
import de.eva.forecastr.rest.CommunicationHandler;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class AdminRestHandler {
  private final CommunicationHandler communicationHandler;

  public AdminRestHandler(CommunicationHandler communicationHandler) {
    this.communicationHandler = communicationHandler;
  }

  public AdminStats getStats() {
    return communicationHandler.value(communicationHandler.get("/stats"), AdminStats.class);
  }

  public ImportReport importEvents(String path) {
    String query =
        path == null || path.isBlank()
            ? ""
            : "?path=" + URLEncoder.encode(path.trim(), StandardCharsets.UTF_8);
    return communicationHandler.value(
        communicationHandler.post("/admin/import" + query, Map.of()), ImportReport.class);
  }

  public TestUsersResponse seedTestUsers(
      int count, int betsPerUser, Long eventId, String outcome, BigDecimal stake) {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put("count", count);
    request.put("betsPerUser", betsPerUser);
    if (eventId != null) {
      request.put("eventId", eventId);
    }
    if (outcome != null) {
      request.put("outcome", outcome);
    }
    if (stake != null) {
      request.put("stake", stake);
    }
    return communicationHandler.value(
        communicationHandler.post("/admin/test-data/users", request), TestUsersResponse.class);
  }

  public TestEventsResponse seedTestEvents(int count, Integer expiresInMinutes) {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put("count", count);
    if (expiresInMinutes != null) {
      request.put("expiresInMinutes", expiresInMinutes);
    }
    return communicationHandler.value(
        communicationHandler.post("/admin/test-data/events", request), TestEventsResponse.class);
  }

  public ManualResolutionResult resolve(long eventId, String outcome) {
    return communicationHandler.value(
        communicationHandler.post(
            "/admin/events/" + eventId + "/resolve", Map.of("outcome", outcome)),
        ManualResolutionResult.class);
  }
}
