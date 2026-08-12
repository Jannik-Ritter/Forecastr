package de.eva.forecastr.core.models;

import java.math.BigDecimal;

public record LiveUpdate(
    Type type, long eventId, String action, String kind, BigDecimal amount, String detail) {
  public enum Type {
    FEED,
    NOTIFICATION,
    CONNECTION_ERROR
  }
}
