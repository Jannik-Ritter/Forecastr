package de.eva.forecastr.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import de.eva.forecastr.core.models.LiveUpdate;
import de.eva.forecastr.rest.CommunicationHandler;
import java.math.BigDecimal;
import java.net.http.WebSocket;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class WebSocketClient {
  private final CommunicationHandler communicationHandler;

  public WebSocketClient(CommunicationHandler communicationHandler) {
    this.communicationHandler = communicationHandler;
  }

  public AutoCloseable subscribeToLiveUpdates(long userId, Consumer<LiveUpdate> updates) {
    LiveSockets sockets = new LiveSockets();
    subscribe("/topic/feed", updates, sockets);
    return sockets;
  }

  private void subscribe(String topic, Consumer<LiveUpdate> updates, LiveSockets sockets) {
    communicationHandler
        .subscribe(topic, message -> parseLiveUpdate(message).ifPresent(updates))
        .thenAccept(sockets::add)
        .exceptionally(
            error -> {
              updates.accept(connectionError(error.getMessage()));
              return null;
            });
  }

  private Optional<LiveUpdate> parseLiveUpdate(String message) {
    try {
      JsonNode response = communicationHandler.json(message);
      return switch (response.path("type").asText()) {
        case "FEED" ->
            Optional.of(
                new LiveUpdate(
                    LiveUpdate.Type.FEED,
                    response.path("eventId").asLong(),
                    response.path("action").asText(),
                    null,
                    BigDecimal.ZERO,
                    null));
        case "ERROR" -> Optional.of(connectionError(response.path("message").asText()));
        default -> Optional.empty();
      };
    } catch (IllegalArgumentException exception) {
      if (message != null && message.startsWith("WebSocket error:")) {
        return Optional.of(connectionError(message));
      }
      return Optional.empty();
    }
  }

  private LiveUpdate connectionError(String detail) {
    return new LiveUpdate(LiveUpdate.Type.CONNECTION_ERROR, 0, null, null, BigDecimal.ZERO, detail);
  }

  private static final class LiveSockets implements AutoCloseable {
    private final List<WebSocket> sockets = new CopyOnWriteArrayList<>();
    private volatile boolean isClosed;

    private void add(WebSocket socket) {
      if (isClosed) {
        socket.sendClose(WebSocket.NORMAL_CLOSURE, "client closed");
      } else {
        sockets.add(socket);
      }
    }

    @Override
    public void close() {
      isClosed = true;
      for (WebSocket socket : sockets) {
        socket.sendClose(WebSocket.NORMAL_CLOSURE, "client closed");
      }
      sockets.clear();
    }
  }
}
