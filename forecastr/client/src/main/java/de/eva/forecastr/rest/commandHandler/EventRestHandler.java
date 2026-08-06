package de.eva.forecastr.rest.commandHandler;

import de.eva.forecastr.core.models.Market;
import de.eva.forecastr.rest.CommunicationHandler;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class EventRestHandler {
  private final CommunicationHandler communicationHandler;

  public EventRestHandler(CommunicationHandler communicationHandler) {
    this.communicationHandler = communicationHandler;
  }

  public List<Market> getFeed() {
    return communicationHandler.values(communicationHandler.get("/feed?limit=200"), Market.class);
  }

  public List<Market> searchEvents(String text, String status) {
    List<String> query = new ArrayList<>();
    addQueryParameter(query, "name", text);
    addQueryParameter(query, "status", status);
    String path = "/events" + (query.isEmpty() ? "" : "?" + String.join("&", query));
    return communicationHandler.values(communicationHandler.get(path), Market.class);
  }

  public Market getEvent(long eventId) {
    return communicationHandler.value(communicationHandler.get("/events/" + eventId), Market.class);
  }

  private void addQueryParameter(List<String> query, String name, String value) {
    if (value != null && !value.isBlank()) {
      query.add(name + "=" + URLEncoder.encode(value.trim(), StandardCharsets.UTF_8));
    }
  }
}
