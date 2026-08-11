package de.eva.forecastr.config;

import de.eva.forecastr.core.interfaces.ForecastrEventPublisher;
import de.eva.forecastr.core.models.EventStatus;
import de.eva.forecastr.core.models.events.EventChanged;
import de.eva.forecastr.core.models.events.ImportsRejected;
import de.eva.forecastr.core.models.events.ResolutionRecorded;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringForecastrEventPublisher implements ForecastrEventPublisher {
  private final ApplicationEventPublisher eventPublisher;

  public SpringForecastrEventPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void eventChanged(Long eventId, String action) {
    eventPublisher.publishEvent(new EventChanged(eventId, action));
  }

  @Override
  public void importsRejected(long count) {
    if (count > 0) {
      eventPublisher.publishEvent(new ImportsRejected(count));
    }
  }

  @Override
  public void resolutionRecorded(EventStatus status) {
    eventPublisher.publishEvent(new ResolutionRecorded(status));
  }
}
