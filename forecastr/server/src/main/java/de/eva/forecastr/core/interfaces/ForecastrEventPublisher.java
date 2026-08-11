package de.eva.forecastr.core.interfaces;

import de.eva.forecastr.core.models.EventStatus;

public interface ForecastrEventPublisher {
  void eventChanged(Long eventId, String action);

  void importsRejected(long count);

  void resolutionRecorded(EventStatus status);
}
