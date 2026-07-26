package de.eva.forecastr.core.interfaces;

public interface ForecastrEventPublisher {
  void eventChanged(Long eventId, String action);
}
