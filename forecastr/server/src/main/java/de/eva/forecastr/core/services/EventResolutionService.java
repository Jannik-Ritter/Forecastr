package de.eva.forecastr.core.services;

import de.eva.forecastr.core.interfaces.EventResolver;
import de.eva.forecastr.core.interfaces.ForecastrEventPublisher;
import de.eva.forecastr.core.models.EventStatus;
import de.eva.forecastr.core.models.LogType;
import de.eva.forecastr.core.models.MarketEvent;
import de.eva.forecastr.core.models.Outcome;
import de.eva.forecastr.core.models.PlannedResolution;
import de.eva.forecastr.core.models.ResolutionResult;
import de.eva.forecastr.core.models.exceptions.ForecastrException;
import de.eva.forecastr.repository.EventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EventResolutionService implements EventResolver {
  private final EventRepository eventRepository;
  private final PayoutService payoutService;
  private final LogService logService;
  private final ForecastrEventPublisher eventPublisher;
  private final Clock clock;
  private final TransactionTemplate transactionTemplate;

  public EventResolutionService(
      EventRepository eventRepository,
      PayoutService payoutService,
      LogService logService,
      ForecastrEventPublisher eventPublisher,
      Clock clock,
      TransactionTemplate transactionTemplate) {
    this.eventRepository = eventRepository;
    this.payoutService = payoutService;
    this.logService = logService;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
    this.transactionTemplate = transactionTemplate;
  }

  @Override
  public ResolutionResult resolve(Long eventId) {
    return Objects.requireNonNull(transactionTemplate.execute(status -> resolveLocked(eventId)));
  }

  @Scheduled(fixedDelayString = "${forecastr.resolver-delay-ms:10000}")
  public void resolveDueEvents() {
    for (Long eventId : eventRepository.findDueIds(clock.instant())) {
      try {
        resolve(eventId);
      } catch (RuntimeException exception) {
        logService.log(
            LogType.RESOLUTION, Map.of("eventId", eventId, "error", exception.getMessage()));
      }
    }
  }

  private ResolutionResult resolveLocked(Long eventId) {
    MarketEvent event =
        eventRepository
            .findLocked(eventId)
            .orElseThrow(() -> ForecastrException.notFound("Resource not found"));
    if (event.getStatus() != EventStatus.OPEN) {
      return ResolutionResult.unchanged(eventId, event.getStatus());
    }
    Instant now = clock.instant();
    if (event.getPlannedResolution() == PlannedResolution.UNRESOLVABLE) {
      if (now.isBefore(event.getClosesAt())) {
        return ResolutionResult.unchanged(eventId, event.getStatus());
      }
      event.expire(now);
      return finish(event, payoutService.refund(event), "EXPIRED");
    }
    if (now.isBefore(event.getPlannedResolutionAt())) {
      return ResolutionResult.unchanged(eventId, event.getStatus());
    }
    Outcome outcome = Outcome.valueOf(event.getPlannedResolution().name());
    event.resolve(outcome, now);
    return finish(event, payoutService.settle(event, outcome), "RESOLVED_" + outcome);
  }

  private ResolutionResult finish(
      MarketEvent event, ResolutionResult result, String feedAction) {
    eventRepository.save(event);
    logService.log(
        LogType.RESOLUTION, Map.of("eventId", event.getId(), "result", feedAction));
    eventPublisher.resolutionRecorded(event.getStatus());
    eventPublisher.eventChanged(event.getId(), feedAction);
    return new ResolutionResult(
        event.getId(),
        event.getStatus(),
        result.winners(),
        result.losers(),
        result.payouts(),
        result.fees(),
        true);
  }
}
