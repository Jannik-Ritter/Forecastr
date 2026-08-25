package de.eva.forecastr.core.services;

import de.eva.forecastr.core.interfaces.ForecastrEventPublisher;
import de.eva.forecastr.core.models.EventStatus;
import de.eva.forecastr.core.models.MarketEvent;
import de.eva.forecastr.core.models.Money;
import de.eva.forecastr.core.models.Outcome;
import de.eva.forecastr.core.models.PlannedResolution;
import de.eva.forecastr.core.models.SeedResult;
import de.eva.forecastr.core.models.TestEventsResult;
import de.eva.forecastr.core.models.TestUsersResult;
import de.eva.forecastr.core.models.User;
import de.eva.forecastr.core.models.Wallet;
import de.eva.forecastr.core.models.exceptions.ForecastrException;
import de.eva.forecastr.repository.EventRepository;
import de.eva.forecastr.repository.UserRepository;
import de.eva.forecastr.repository.WalletRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeedService {
  private static final BigDecimal DEFAULT_INITIAL_BALANCE = new BigDecimal("100000.00");
  private static final BigDecimal TEST_ACCOUNT_REMAINDER = new BigDecimal("100.00");
  private static final AtomicLong NEXT_EVENT_ID =
      new AtomicLong(Math.max(1_000_000L, System.currentTimeMillis()));
  private static final List<String> MARKET_QUESTIONS =
      List.of(
          "Steigt die Temperatur in Leipzig heute über 25 Grad?",
          "Erreicht der Livestream heute 1.000 Zuschauer?",
          "Kommt der nächste ICE nach Leipzig pünktlich an?",
          "Schließt die Universitätsbibliothek heute später als geplant?",
          "Gewinnt Dresden das nächste Eishockeyspiel?",
          "Gibt es heute eine Unwetterwarnung für Sachsen?",
          "Übersteigt der Bitcoin-Kurs heute sein bisheriges Tageshoch?",
          "Ist der Augustusplatz heute Abend für den Verkehr gesperrt?",
          "Veröffentlicht die Universität heute eine Pressemitteilung?",
          "Beginnt das heutige Konzert pünktlich?");

  private final UserRepository userRepository;
  private final WalletRepository walletRepository;
  private final EventRepository eventRepository;
  private final BetService betService;
  private final ForecastrEventPublisher eventPublisher;
  private final Clock clock;

  public SeedService(
      UserRepository userRepository,
      WalletRepository walletRepository,
      EventRepository eventRepository,
      BetService betService,
      ForecastrEventPublisher eventPublisher,
      Clock clock) {
    this.userRepository = userRepository;
    this.walletRepository = walletRepository;
    this.eventRepository = eventRepository;
    this.betService = betService;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  @Transactional
  public SeedResult seed(int userCount, int eventCount, BigDecimal initialBalance) {
    String nonce = Long.toUnsignedString(System.nanoTime());
    BigDecimal balance =
        initialBalance == null ? DEFAULT_INITIAL_BALANCE : Money.amount(initialBalance);
    List<Long> userIds = new ArrayList<>(userCount);
    for (int index = 0; index < userCount; index++) {
      User user = createUser("load-" + nonce + "-" + index, balance);
      userIds.add(user.getId());
    }
    List<Long> eventIds = new ArrayList<>(eventCount);
    for (int index = 0; index < eventCount; index++) {
      Instant now = clock.instant();
      MarketEvent event =
          createEvent(
              question(index),
              now,
              now.plus(Duration.ofHours(1)),
              PlannedResolution.YES,
              now.plus(Duration.ofMinutes(30)));
      eventIds.add(event.getId());
    }
    return new SeedResult(userIds, eventIds);
  }

  @Transactional
  public TestUsersResult seedTestUsers(
      int count, int betsPerUser, Long eventId, Outcome outcome, BigDecimal fixedStake) {
    validateTestUsers(count, betsPerUser, eventId, fixedStake);
    Instant operationTime = clock.instant();
    BigDecimal normalizedStake = fixedStake == null ? null : Money.amount(fixedStake);
    List<MarketEvent> candidates = eligibleEvents(eventId, betsPerUser, operationTime);
    String nonce = Long.toUnsignedString(System.nanoTime());
    List<Long> userIds = new ArrayList<>(count);
    List<Long> betIds = new ArrayList<>();
    for (int userIndex = 0; userIndex < count; userIndex++) {
      List<PlannedBet> plannedBets = planBets(candidates, betsPerUser, outcome, normalizedStake);
      BigDecimal totalStake =
          plannedBets.stream().map(PlannedBet::stake).reduce(Money.ZERO, BigDecimal::add);
      User user =
          createUser(
              "test-" + nonce + "-" + userIndex,
              Money.amount(totalStake.add(TEST_ACCOUNT_REMAINDER)),
              operationTime);
      userIds.add(user.getId());
      for (PlannedBet plannedBet : plannedBets) {
        betIds.add(
            betService
                .placeBetAt(
                    plannedBet.eventId(),
                    user.getId(),
                    plannedBet.outcome(),
                    plannedBet.stake(),
                    operationTime)
                .getId());
      }
    }
    return new TestUsersResult(userIds, betIds);
  }

  @Transactional
  public TestEventsResult seedTestEvents(int count, Integer expiresInMinutes) {
    if (count < 1) {
      throw new IllegalArgumentException("count must be at least 1");
    }
    int lifetime = expiresInMinutes == null ? 10 : expiresInMinutes;
    if (lifetime < 1 || lifetime > 1440) {
      throw new IllegalArgumentException("expiresInMinutes must be between 1 and 1440");
    }
    Instant now = clock.instant();
    Instant closesAt = now.plus(Duration.ofMinutes(lifetime));
    List<Long> eventIds = new ArrayList<>(count);
    for (int index = 0; index < count; index++) {
      PlannedResolution resolution =
          randomOutcome() == Outcome.YES ? PlannedResolution.YES : PlannedResolution.NO;
      MarketEvent event = createEvent(question(index), now, closesAt, resolution, closesAt);
      eventIds.add(event.getId());
    }
    return new TestEventsResult(eventIds);
  }

  private List<PlannedBet> planBets(
      List<MarketEvent> candidates, int betsPerUser, Outcome outcome, BigDecimal fixedStake) {
    List<PlannedBet> plannedBets = new ArrayList<>(betsPerUser);
    for (int index = 0; index < betsPerUser; index++) {
      MarketEvent event = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
      Outcome selectedOutcome = outcome == null ? randomOutcome() : outcome;
      BigDecimal stake = fixedStake == null ? randomStake() : fixedStake;
      plannedBets.add(new PlannedBet(event.getId(), selectedOutcome, stake));
    }
    return plannedBets;
  }

  private User createUser(String username, BigDecimal balance) {
    return createUser(username, balance, clock.instant());
  }

  private User createUser(String username, BigDecimal balance, Instant createdAt) {
    User user = userRepository.save(new User(username, createdAt, false));
    walletRepository.save(new Wallet(user.getId(), balance));
    return user;
  }

  private MarketEvent createEvent(
      String question,
      Instant createdAt,
      Instant closesAt,
      PlannedResolution resolution,
      Instant resolutionAt) {
    MarketEvent event =
        eventRepository.save(
            new MarketEvent(
                nextEventId(), question, createdAt, closesAt, resolution, resolutionAt));
    eventPublisher.eventChanged(event.getId(), "IMPORTED");
    return event;
  }

  private List<MarketEvent> eligibleEvents(Long eventId, int betsPerUser, Instant now) {
    if (betsPerUser == 0) {
      return List.of();
    }
    if (eventId == null) {
      List<MarketEvent> candidates = eventRepository.findBettable(now);
      if (candidates.isEmpty()) {
        throw ForecastrException.conflict("No open events available for test bets");
      }
      return candidates;
    }
    MarketEvent event =
        eventRepository
            .findById(eventId)
            .orElseThrow(() -> ForecastrException.notFound("Event not found"));
    if (event.getStatus() != EventStatus.OPEN
        || event.getCreatedAt().isAfter(now)
        || !event.getClosesAt().isAfter(now)) {
      throw ForecastrException.conflict("Selected event is not open for betting");
    }
    return List.of(event);
  }

  private void validateTestUsers(int count, int betsPerUser, Long eventId, BigDecimal fixedStake) {
    if (count < 1) {
      throw new IllegalArgumentException("count must be at least 1");
    }
    if (betsPerUser < 0) {
      throw new IllegalArgumentException("betsPerUser cannot be negative");
    }
    if (eventId != null && eventId <= 0) {
      throw new IllegalArgumentException("eventId must be positive");
    }
    if (fixedStake != null && Money.amount(fixedStake).signum() <= 0) {
      throw new IllegalArgumentException("stake must be positive");
    }
  }

  private long nextEventId() {
    long eventId;
    do {
      eventId = NEXT_EVENT_ID.getAndIncrement();
    } while (eventRepository.existsById(eventId));
    return eventId;
  }

  private String question(int index) {
    return MARKET_QUESTIONS.get(index % MARKET_QUESTIONS.size());
  }

  private Outcome randomOutcome() {
    return ThreadLocalRandom.current().nextBoolean() ? Outcome.YES : Outcome.NO;
  }

  private BigDecimal randomStake() {
    return BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(100, 10_001), 2);
  }

  private record PlannedBet(Long eventId, Outcome outcome, BigDecimal stake) {}
}
