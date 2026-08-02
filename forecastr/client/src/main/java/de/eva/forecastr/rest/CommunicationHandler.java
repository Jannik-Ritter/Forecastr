package de.eva.forecastr.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.eva.forecastr.core.models.exceptions.ClientException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class CommunicationHandler {
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

  private final URI baseUri;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public CommunicationHandler(String baseUrl) {
    this.baseUri =
        URI.create(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    this.objectMapper =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  public RestResponse get(String path) {
    return send("GET", path, null, DEFAULT_TIMEOUT);
  }

  public RestResponse post(String path, Object body) {
    return send("POST", path, body, DEFAULT_TIMEOUT);
  }

  public RestResponse put(String path, Object body) {
    return send("PUT", path, body, DEFAULT_TIMEOUT);
  }

  public RestResponse delete(String path) {
    return send("DELETE", path, null, DEFAULT_TIMEOUT);
  }

  public RestResponse send(String method, String path, Object body, Duration timeout) {
    try {
      String payload =
          body == null
              ? null
              : body instanceof String text ? text : objectMapper.writeValueAsString(body);
      HttpRequest.Builder request =
          HttpRequest.newBuilder(baseUri.resolve(path))
              .timeout(timeout)
              .header("Accept", "application/json");
      if (payload != null) {
        request.header("Content-Type", "application/json");
      }
      request.method(
          method,
          payload == null
              ? HttpRequest.BodyPublishers.noBody()
              : HttpRequest.BodyPublishers.ofString(payload));
      long startedAt = System.nanoTime();
      HttpResponse<String> response =
          httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
      return new RestResponse(
          response.statusCode(), response.body(), System.nanoTime() - startedAt);
    } catch (HttpTimeoutException exception) {
      return new RestResponse(0, "timeout", timeout.toNanos());
    } catch (IOException exception) {
      return new RestResponse(-1, exception.getMessage(), 0);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return new RestResponse(-1, "interrupted", 0);
    }
  }

  public JsonNode tree(RestResponse response) {
    return json(response.body());
  }

  public JsonNode json(String value) {
    try {
      return objectMapper.readTree(value);
    } catch (Exception exception) {
      throw new IllegalArgumentException("Invalid JSON response: " + value, exception);
    }
  }

  public List<JsonNode> array(RestResponse response) {
    JsonNode array = tree(response);
    if (!array.isArray()) {
      throw new IllegalArgumentException("Expected array: " + response.body());
    }
    List<JsonNode> values = new ArrayList<>();
    array.forEach(values::add);
    return values;
  }

  public <T> T value(RestResponse response, Class<T> type) {
    requireSuccess(response);
    try {
      return objectMapper.treeToValue(tree(response), type);
    } catch (Exception exception) {
      throw new ClientException(-1, "Der Server hat eine unverständliche Antwort gesendet.");
    }
  }

  public <T> List<T> values(RestResponse response, Class<T> type) {
    requireSuccess(response);
    try {
      List<T> values = new ArrayList<>();
      for (JsonNode node : array(response)) {
        values.add(objectMapper.treeToValue(node, type));
      }
      return values;
    } catch (Exception exception) {
      throw new ClientException(-1, "Der Server hat eine unverständliche Antwort gesendet.");
    }
  }

  public void requireSuccess(RestResponse response) {
    if (!response.isSuccess()) {
      throw new ClientException(response.status(), germanError(response));
    }
  }

  private String germanError(RestResponse response) {
    if (response.status() == 0) {
      return "Der Server hat nicht rechtzeitig geantwortet.";
    }
    if (response.status() < 0) {
      return "Der Forecastr-Server ist nicht erreichbar.";
    }
    String message = "";
    try {
      message = tree(response).path("message").asText();
    } catch (IllegalArgumentException malformedErrorResponse) {
      message = "";
    }
    return switch (message) {
      case "Username already exists" -> "Dieser Benutzername ist bereits vergeben.";
      case "Account has open bets" ->
          "Das Konto kann erst gelöscht werden, wenn alle offenen Wetten abgeschlossen" + " sind.";
      case "Insufficient balance" -> "Das Guthaben reicht für diesen Betrag nicht aus.";
      case "Event is not open for betting" -> "Dieser Markt ist nicht mehr für Wetten geöffnet.";
      case "Stake must be positive", "Amount must be positive" ->
          "Der Betrag muss größer als 0 sein.";
      case "User not found" -> "Das Benutzerkonto wurde nicht gefunden.";
      case "Event not found" -> "Der Markt wurde nicht gefunden.";
      case "Admin account required" -> "Für diese Aktion ist ein Administratorkonto erforderlich.";
      case "Server is busy; retry the request" ->
          "Der Server ist gerade ausgelastet. Bitte versuche es erneut.";
      default -> "Die Anfrage konnte nicht ausgeführt werden (Fehler " + response.status() + ").";
    };
  }

}
