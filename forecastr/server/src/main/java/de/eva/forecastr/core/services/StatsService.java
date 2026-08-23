package de.eva.forecastr.core.services;

import de.eva.forecastr.core.models.EventStatus;
import de.eva.forecastr.core.models.Money;
import de.eva.forecastr.core.models.StatsSnapshot;
import de.eva.forecastr.repository.BetRepository;
import de.eva.forecastr.repository.EventRepository;
import de.eva.forecastr.repository.EventStatusCount;
import de.eva.forecastr.repository.FeeRevenueRepository;
import de.eva.forecastr.repository.UserBetCount;
import de.eva.forecastr.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatsService {
  private static final Logger LOGGER = LoggerFactory.getLogger(StatsService.class);

  private final EventRepository eventRepository;
  private final BetRepository betRepository;
  private final FeeRevenueRepository feeRevenueRepository;
  private final UserRepository userRepository;
  private final PlatformMetrics platformMetrics;

  public StatsService(
      EventRepository eventRepository,
      BetRepository betRepository,
      FeeRevenueRepository feeRevenueRepository,
      UserRepository userRepository,
      PlatformMetrics platformMetrics) {
    this.eventRepository = eventRepository;
    this.betRepository = betRepository;
    this.feeRevenueRepository = feeRevenueRepository;
    this.userRepository = userRepository;
    this.platformMetrics = platformMetrics;
  }

  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public StatsSnapshot getStats() {
    LOGGER.info("STATS operation on thread {}", Thread.currentThread().getName());
    Map<String, Long> eventsByStatus = new LinkedHashMap<>();
    for (EventStatus status : EventStatus.values()) {
      eventsByStatus.put(status.name(), 0L);
    }
    for (EventStatusCount statusCount : eventRepository.countByStatus()) {
      eventsByStatus.put(statusCount.status().name(), statusCount.count());
    }
    Map<Long, Long> perUserBetCount = new TreeMap<>();
    for (UserBetCount userBetCount : betRepository.countByUser()) {
      perUserBetCount.put(userBetCount.userId(), userBetCount.count());
    }
    Map<String, Long> resolver = new LinkedHashMap<>();
    resolver.put("resolved", platformMetrics.resolvedCount());
    resolver.put("expired", platformMetrics.expiredCount());
    resolver.put("rejectedImports", platformMetrics.rejectedImportCount());
    return new StatsSnapshot(
        userRepository.countByDeletedAtIsNull(),
        eventRepository.count(),
        eventsByStatus,
        betRepository.count(),
        Money.amount(betRepository.sumPayouts()),
        Money.amount(feeRevenueRepository.sumAmount()),
        perUserBetCount,
        resolver);
  }
}
