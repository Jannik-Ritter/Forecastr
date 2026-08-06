package de.eva.forecastr.core.services;

import de.eva.forecastr.core.models.EventSnapshot;
import de.eva.forecastr.core.models.EventStatus;
import de.eva.forecastr.core.models.MarketEvent;
import de.eva.forecastr.core.models.Money;
import de.eva.forecastr.core.models.Outcome;
import de.eva.forecastr.core.models.exceptions.ForecastrException;
import de.eva.forecastr.repository.BetRepository;
import de.eva.forecastr.repository.EventPoolTotal;
import de.eva.forecastr.repository.EventRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {
  private final EventRepository eventRepository;
  private final BetRepository betRepository;
  private final Clock clock;

  public EventService(EventRepository eventRepository, BetRepository betRepository, Clock clock) {
    this.eventRepository = eventRepository;
    this.betRepository = betRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public List<EventSnapshot> searchEvents(String name, EventStatus status, Instant endsBefore) {
    List<MarketEvent> events =
        eventRepository.findAll(
            (root, query, criteriaBuilder) -> {
              List<Predicate> predicates = new ArrayList<>();
              if (name != null && !name.isBlank()) {
                predicates.add(
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("question")),
                        "%" + name.toLowerCase(Locale.ROOT) + "%"));
              }
              if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
              }
              if (endsBefore != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("closesAt"), endsBefore));
              }
              return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
            },
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("closesAt")));
    return snapshots(events);
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public EventSnapshot getEventById(Long eventId) {
    MarketEvent event =
        eventRepository
            .findById(eventId)
            .orElseThrow(() -> ForecastrException.notFound("Event not found"));
    return snapshots(List.of(event)).getFirst();
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public List<EventSnapshot> getFeed(int limit) {
    int safeLimit = Math.max(1, Math.min(limit, 200));
    List<MarketEvent> events =
        eventRepository.findFeed(clock.instant(), PageRequest.of(0, safeLimit));
    return snapshots(events);
  }

  private List<EventSnapshot> snapshots(List<MarketEvent> events) {
    if (events.isEmpty()) {
      return List.of();
    }
    List<Long> eventIds = events.stream().map(MarketEvent::getId).toList();
    Map<Long, BigDecimal> yesPools = new HashMap<>();
    Map<Long, BigDecimal> noPools = new HashMap<>();
    for (EventPoolTotal total : betRepository.sumPools(eventIds)) {
      Map<Long, BigDecimal> pools = total.outcome() == Outcome.YES ? yesPools : noPools;
      pools.put(total.eventId(), Money.amount(total.total()));
    }
    List<EventSnapshot> snapshots = new ArrayList<>(events.size());
    for (MarketEvent event : events) {
      snapshots.add(
          new EventSnapshot(
              event.getId(),
              event.getQuestion(),
              event.getCreatedAt(),
              event.getClosesAt(),
              event.getPlannedResolutionAt() == null
                  ? event.getClosesAt()
                  : event.getPlannedResolutionAt(),
              event.getStatus(),
              event.getResolvedAt(),
              yesPools.getOrDefault(event.getId(), Money.ZERO),
              noPools.getOrDefault(event.getId(), Money.ZERO)));
    }
    return List.copyOf(snapshots);
  }
}
