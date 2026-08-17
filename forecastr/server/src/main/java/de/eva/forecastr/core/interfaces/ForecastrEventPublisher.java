package de.eva.forecastr.core.interfaces;

import de.eva.forecastr.core.models.EventStatus;
import java.math.BigDecimal;

public interface ForecastrEventPublisher {
  void eventChanged(Long eventId, String action);

  void userNotification(Long userId, Long eventId, String kind, BigDecimal amount);

  void importsRejected(long count);

  void resolutionRecorded(EventStatus status);
}
