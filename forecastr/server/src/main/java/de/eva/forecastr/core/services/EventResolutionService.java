package de.eva.forecastr.core.services;

import de.eva.forecastr.core.interfaces.EventResolver;
import de.eva.forecastr.core.interfaces.ForecastrEventPublisher;
import de.eva.forecastr.core.models.EventStatus;
import de.eva.forecastr.core.models.LogType;
import de.eva.forecastr.core.models.ManualResolution;
import de.eva.forecastr.core.models.MarketEvent;
import de.eva.forecastr.core.models.Outcome;
import de.eva.forecastr.core.models.PlannedResolution;
import de.eva.forecastr.core.models.ResolutionResult;
import de.eva.forecastr.core.models.exceptions.ForecastrException;
import de.eva.forecastr.repository.EventRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
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
  private final Duration retention;

  public EventResolutionService(
      EventRepository eventRepository,
      PayoutService payoutService,
      LogService logService,
      ForecastrEventPublisher eventPublisher,
      Clock clock,
      TransactionTemplate transactionTemplate,
      @Value("${forecastr.archive-retention:PT60M}") Duration retention) {
    this.eventRepository = eventRepository;
    this.payoutService = payoutService;
    this.logService = logService;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
    this.transactionTemplate = transactionTemplate;
    this.retention = retention;
  }

  @Override
  public ResolutionResult resolve(Long eventId) {
    return inTransaction(eventId, ResolutionRequest.automatic());
  }

  @Override
  public ResolutionResult resolveManually(
      Long eventId, ManualResolution resolution, Long actorUserId) {
    return inTransaction(eventId, ResolutionRequest.manual(resolution, actorUserId));
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

  @Scheduled(fixedDelayString = "${forecastr.archive-delay-ms:60000}")
  public void archiveOldEvents() {
    Instant cutoff = clock.instant().minus(retention);
    for (Long eventId : eventRepository.findArchivableIds(cutoff)) {
      transactionTemplate.executeWithoutResult(status -> archive(eventId));
    }
  }

  private ResolutionResult inTransaction(Long eventId, ResolutionRequest request) {
    return Objects.requireNonNull(
        transactionTemplate.execute(status -> resolveInTransaction(eventId, request)));
  }

  private ResolutionResult resolveInTransaction(Long eventId, ResolutionRequest request) {
    MarketEvent event =
        eventRepository
            .findLocked(eventId)
            .orElseThrow(() -> ForecastrException.notFound("Resource not found"));
    if (event.getStatus() != EventStatus.OPEN) {
      return ResolutionResult.unchanged(eventId, event.getStatus());
    }
    Instant now = clock.instant();
    ResolutionDecision decision = decide(event, request, now);
    if (decision == null) {
      return ResolutionResult.unchanged(eventId, event.getStatus());
    }
    ResolutionResult payoutResult;
    if (decision.isRefund()) {
      event.expire(now);
      payoutResult = payoutService.refund(event);
    } else {
      event.resolve(decision.outcome(), now);
      payoutResult = payoutService.settle(event, decision.outcome());
    }
    eventRepository.save(event);
    logResolution(eventId, request, decision);
    eventPublisher.resolutionRecorded(event.getStatus());
    eventPublisher.eventChanged(eventId, decision.feedAction());
    return new ResolutionResult(
        eventId,
        event.getStatus(),
        payoutResult.winners(),
        payoutResult.losers(),
        payoutResult.payouts(),
        payoutResult.fees(),
        true);
  }

  private ResolutionDecision decide(MarketEvent event, ResolutionRequest request, Instant now) {
    if (request.manualResolution() != null) {
      if (request.manualResolution() == ManualResolution.REFUND) {
        return ResolutionDecision.refund("REFUND");
      }
      Outcome outcome = Outcome.valueOf(request.manualResolution().name());
      return ResolutionDecision.settle(outcome);
    }
    if (event.getPlannedResolution() == PlannedResolution.UNRESOLVABLE) {
      return now.isBefore(event.getClosesAt()) ? null : ResolutionDecision.refund("EXPIRED");
    }
    if (now.isBefore(event.getPlannedResolutionAt())) {
      return null;
    }
    return ResolutionDecision.settle(Outcome.valueOf(event.getPlannedResolution().name()));
  }

  private void logResolution(Long eventId, ResolutionRequest request, ResolutionDecision decision) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("eventId", eventId);
    payload.put("result", decision.logResult());
    if (request.actorUserId() != null) {
      payload.put("source", "ADMIN");
      payload.put("actorUserId", request.actorUserId());
    }
    logService.log(LogType.RESOLUTION, payload);
  }

  private void archive(Long eventId) {
    MarketEvent event = eventRepository.findLocked(eventId).orElse(null);
    if (event == null || event.getStatus() == EventStatus.ARCHIVED) {
      return;
    }
    event.archive();
    eventRepository.save(event);
    eventPublisher.eventChanged(eventId, "ARCHIVED");
  }

  private record ResolutionRequest(ManualResolution manualResolution, Long actorUserId) {
    private static ResolutionRequest automatic() {
      return new ResolutionRequest(null, null);
    }

    private static ResolutionRequest manual(ManualResolution resolution, Long actorUserId) {
      return new ResolutionRequest(resolution, actorUserId);
    }
  }

  private record ResolutionDecision(
      boolean isRefund, Outcome outcome, Object logResult, String feedAction) {
    private static ResolutionDecision refund(String logResult) {
      return new ResolutionDecision(true, null, logResult, "EXPIRED");
    }

    private static ResolutionDecision settle(Outcome outcome) {
      return new ResolutionDecision(false, outcome, outcome, "RESOLVED_" + outcome);
    }
  }
}
