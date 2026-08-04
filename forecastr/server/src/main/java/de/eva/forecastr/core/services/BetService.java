package de.eva.forecastr.core.services;

import de.eva.forecastr.core.models.Bet;
import de.eva.forecastr.core.models.EventStatus;
import de.eva.forecastr.core.models.LogType;
import de.eva.forecastr.core.models.MarketEvent;
import de.eva.forecastr.core.models.Money;
import de.eva.forecastr.core.models.Outcome;
import de.eva.forecastr.core.models.User;
import de.eva.forecastr.core.models.Wallet;
import de.eva.forecastr.core.models.exceptions.ForecastrException;
import de.eva.forecastr.repository.BetRepository;
import de.eva.forecastr.repository.EventRepository;
import de.eva.forecastr.repository.UserRepository;
import de.eva.forecastr.repository.WalletRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BetService {
  private final EventRepository eventRepository;
  private final UserRepository userRepository;
  private final WalletRepository walletRepository;
  private final BetRepository betRepository;
  private final LogService logService;
  private final Clock clock;

  public BetService(
      EventRepository eventRepository,
      UserRepository userRepository,
      WalletRepository walletRepository,
      BetRepository betRepository,
      LogService logService,
      Clock clock) {
    this.eventRepository = eventRepository;
    this.userRepository = userRepository;
    this.walletRepository = walletRepository;
    this.betRepository = betRepository;
    this.logService = logService;
    this.clock = clock;
  }

  @Transactional
  public Bet placeBet(Long eventId, Long userId, Outcome outcome, BigDecimal stake) {
    BigDecimal normalizedStake = Money.amount(stake);
    if (normalizedStake.signum() <= 0) {
      throw new IllegalArgumentException("Stake must be positive");
    }
    MarketEvent event =
        eventRepository
            .findLocked(eventId)
            .orElseThrow(() -> ForecastrException.notFound("Event not found"));
    User user =
        userRepository
            .findLocked(userId)
            .orElseThrow(() -> ForecastrException.notFound("User not found"));
    if (user.isDeleted()) {
      throw ForecastrException.notFound("User not found");
    }
    Instant now = clock.instant();
    if (event.getStatus() != EventStatus.OPEN
        || now.isBefore(event.getCreatedAt())
        || !now.isBefore(event.getClosesAt())) {
      throw ForecastrException.conflict("Event is not open for betting");
    }
    Wallet wallet =
        walletRepository
            .findLocked(userId)
            .orElseThrow(() -> ForecastrException.notFound("Wallet not found"));
    try {
      wallet.debit(normalizedStake);
    } catch (IllegalStateException exception) {
      throw ForecastrException.paymentRequired("Insufficient balance");
    }
    Bet bet = betRepository.save(new Bet(userId, eventId, outcome, normalizedStake, now));
    walletRepository.save(wallet);
    logService.log(
        LogType.BET,
        Map.of(
            "betId", bet.getId(),
            "eventId", eventId,
            "userId", userId,
            "stake", normalizedStake,
            "outcome", outcome));
    return bet;
  }

  @Transactional(readOnly = true)
  public List<Bet> getBetsByUser(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> ForecastrException.notFound("User not found"));
    if (user.isDeleted()) {
      throw ForecastrException.notFound("User not found");
    }
    return betRepository.findByUserIdOrderByPlacedAtDesc(userId);
  }

  @Transactional(readOnly = true)
  public List<Bet> getBetsByEvent(Long eventId) {
    return betRepository.findByEventIdOrderById(eventId);
  }
}
