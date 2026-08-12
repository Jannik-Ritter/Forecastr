package de.eva.forecastr.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.eva.forecastr.core.models.events.EventChanged;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class TopicWebSocketHandler extends TextWebSocketHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(TopicWebSocketHandler.class);

  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final Map<String, Set<WebSocketSession>> subscribers;
  private final Map<String, Set<String>> sessionTopics;

  public TopicWebSocketHandler(ObjectMapper objectMapper, Clock clock) {
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.subscribers = new ConcurrentHashMap<>();
    this.sessionTopics = new ConcurrentHashMap<>();
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws IOException {
    session.setTextMessageSizeLimit(64 * 1024);
    session.sendMessage(
        json(
            Map.of(
                "type", "CONNECTED",
                "message", "Subscribe to /topic/feed or /topic/users/{id}")));
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message)
      throws IOException {
    JsonNode request = objectMapper.readTree(message.getPayload());
    String action = request.path("action").asText();
    String topic = request.path("topic").asText();
    if (!"subscribe".equals(action) || !isValidTopic(topic)) {
      session.sendMessage(
          json(
              Map.of(
                  "type", "ERROR",
                  "message", "Expected subscribe with a valid topic")));
      return;
    }
    subscribers.computeIfAbsent(topic, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    sessionTopics
        .computeIfAbsent(session.getId(), ignored -> ConcurrentHashMap.newKeySet())
        .add(topic);
    session.sendMessage(json(Map.of("type", "SUBSCRIBED", "topic", topic)));
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    remove(session);
  }

  @Override
  public void handleTransportError(WebSocketSession session, Throwable exception) {
    remove(session);
    try {
      session.close();
    } catch (IOException closeException) {
      LOGGER.debug("WebSocket close failed", closeException);
    }
  }

  @Async("applicationExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void feedChanged(EventChanged event) {
    broadcast(
        "/topic/feed",
        Map.of(
            "type", "FEED",
            "eventId", event.eventId(),
            "action", event.action(),
            "timestamp", clock.instant()));
  }

  private void broadcast(String topic, Object payload) {
    TextMessage message;
    try {
      message = json(payload);
    } catch (IOException exception) {
      LOGGER.debug("WebSocket serialization failed", exception);
      return;
    }
    for (WebSocketSession session : List.copyOf(subscribers.getOrDefault(topic, Set.of()))) {
      send(session, message);
    }
  }

  private void send(WebSocketSession session, TextMessage message) {
    try {
      synchronized (session) {
        if (session.isOpen()) {
          session.sendMessage(message);
        } else {
          remove(session);
        }
      }
    } catch (IOException exception) {
      LOGGER.debug("WebSocket send failed", exception);
      remove(session);
    }
  }

  private TextMessage json(Object value) throws IOException {
    return new TextMessage(objectMapper.writeValueAsString(value));
  }

  private boolean isValidTopic(String topic) {
    return "/topic/feed".equals(topic) || topic.matches("/topic/users/[1-9][0-9]*");
  }

  private void remove(WebSocketSession session) {
    for (String topic : sessionTopics.getOrDefault(session.getId(), Set.of())) {
      Set<WebSocketSession> topicSubscribers = subscribers.get(topic);
      if (topicSubscribers != null) {
        topicSubscribers.remove(session);
      }
    }
    sessionTopics.remove(session.getId());
  }
}
