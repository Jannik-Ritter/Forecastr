package de.eva.forecastr.core.services;

import de.eva.forecastr.core.interfaces.ForecastrEventPublisher;
import de.eva.forecastr.core.models.Bet;
import de.eva.forecastr.core.models.BetStatus;
import de.eva.forecastr.core.models.FeeRevenue;
import de.eva.forecastr.core.models.LogType;
import de.eva.forecastr.core.models.MarketEvent;
import de.eva.forecastr.core.models.Money;
import de.eva.forecastr.core.models.Outcome;
import de.eva.forecastr.core.models.ResolutionResult;
import de.eva.forecastr.core.models.Wallet;
import de.eva.forecastr.repository.BetRepository;
import de.eva.forecastr.repository.FeeRevenueRepository;
import de.eva.forecastr.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayoutService {
  private final BetRepository betRepository;
  private final WalletRepository walletRepository;
  private final FeeRevenueRepository feeRevenueRepository;
  private final FeeService feeService;
  private final LogService logService;
  private final ForecastrEventPublisher eventPublisher;

  public PayoutService(
      BetRepository betRepository,
      WalletRepository walletRepository,
      FeeRevenueRepository feeRevenueRepository,
      FeeService feeService,
      LogService logService,
      ForecastrEventPublisher eventPublisher) {
    this.betRepository = betRepository;
    this.walletRepository = walletRepository;
    this.feeRevenueRepository = feeRevenueRepository;
    this.feeService = feeService;
    this.logService = logService;
    this.eventPublisher = eventPublisher;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public ResolutionResult settle(MarketEvent event, Outcome outcome) {
    List<Bet> openBets = getOpenBets(event.getId());
    List<Bet> winners = openBets.stream().filter(bet -> bet.getOutcome() == outcome).toList();
    if (winners.isEmpty()) {
      for (Bet bet : openBets) {
        bet.lose();
      }
      betRepository.saveAll(openBets);
      return new ResolutionResult(
          event.getId(), event.getStatus(), 0, openBets.size(), Money.ZERO, Money.ZERO, true);
    }

    BigDecimal totalPool = openBets.stream().map(Bet::getStake).reduce(Money.ZERO, BigDecimal::add);
    Map<Long, BigDecimal> winningStakes =
        winners.stream()
            .collect(
                Collectors.toMap(
                    Bet::getId, Bet::getStake, (first, second) -> first, LinkedHashMap::new));
    Map<Long, PayoutCalculator.Share> sharesByBetId =
        PayoutCalculator.calculate(winningStakes, totalPool, feeService.rate()).stream()
            .collect(Collectors.toMap(PayoutCalculator.Share::betId, Function.identity()));
    Map<Long, Wallet> walletsByUserId = lockWallets(winners);
    BigDecimal payoutTotal = Money.ZERO;
    BigDecimal feeTotal = Money.ZERO;
    for (Bet bet : openBets) {
      if (bet.getOutcome() == outcome) {
        PayoutCalculator.Share share = sharesByBetId.get(bet.getId());
        bet.win(share.credited(), share.fee());
        walletsByUserId.get(bet.getUserId()).credit(share.credited());
        payoutTotal = Money.amount(payoutTotal.add(share.credited()));
        feeTotal = Money.amount(feeTotal.add(share.fee()));
        recordPayout(event, bet, share);
      } else {
        bet.lose();
        eventPublisher.userNotification(bet.getUserId(), event.getId(), "LOST", Money.ZERO);
      }
    }
    saveChanges(openBets, walletsByUserId.values());
    return new ResolutionResult(
        event.getId(),
        event.getStatus(),
        winners.size(),
        openBets.size() - winners.size(),
        payoutTotal,
        feeTotal,
        true);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public ResolutionResult refund(MarketEvent event) {
    List<Bet> openBets = getOpenBets(event.getId());
    Map<Long, Wallet> walletsByUserId = lockWallets(openBets);
    BigDecimal refundTotal = Money.ZERO;
    for (Bet bet : openBets) {
      bet.refund();
      walletsByUserId.get(bet.getUserId()).credit(bet.getStake());
      refundTotal = Money.amount(refundTotal.add(bet.getStake()));
      logService.log(LogType.PAYOUT, Map.of("betId", bet.getId(), "refund", bet.getStake()));
      eventPublisher.userNotification(bet.getUserId(), event.getId(), "REFUND", bet.getStake());
    }
    saveChanges(openBets, walletsByUserId.values());
    return new ResolutionResult(
        event.getId(), event.getStatus(), 0, 0, refundTotal, Money.ZERO, true);
  }

  private List<Bet> getOpenBets(Long eventId) {
    List<Bet> openBets = new ArrayList<>();
    for (Bet bet : betRepository.findByEventIdOrderById(eventId)) {
      if (bet.getStatus() == BetStatus.OPEN) {
        openBets.add(bet);
      }
    }
    return openBets;
  }

  private void recordPayout(MarketEvent event, Bet bet, PayoutCalculator.Share share) {
    logService.log(
        LogType.PAYOUT,
        Map.of("betId", bet.getId(), "userId", bet.getUserId(), "amount", share.credited()));
    if (share.fee().signum() > 0) {
      feeRevenueRepository.save(
          new FeeRevenue(bet.getId(), event.getId(), share.fee(), event.getResolvedAt()));
      logService.log(LogType.FEE, Map.of("betId", bet.getId(), "amount", share.fee()));
    }
    eventPublisher.userNotification(bet.getUserId(), event.getId(), "PAYOUT", share.credited());
  }

  private Map<Long, Wallet> lockWallets(Collection<Bet> affectedBets) {
    Map<Long, Wallet> walletsByUserId = new LinkedHashMap<>();
    affectedBets.stream()
        .map(Bet::getUserId)
        .distinct()
        .sorted()
        .forEach(
            userId ->
                walletsByUserId.put(userId, walletRepository.findLocked(userId).orElseThrow()));
    return walletsByUserId;
  }

  private void saveChanges(List<Bet> bets, Collection<Wallet> wallets) {
    walletRepository.saveAll(wallets);
    betRepository.saveAll(bets);
  }
}
