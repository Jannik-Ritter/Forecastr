package de.eva.forecastr.core.services;

import de.eva.forecastr.core.models.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PayoutCalculator {
  private PayoutCalculator() {}

  public record Share(long betId, BigDecimal gross, BigDecimal fee, BigDecimal credited) {}

  private record Allocation(long betId, BigDecimal exact, BigDecimal gross) {}

  public static List<Share> calculate(
      Map<Long, BigDecimal> winningStakes, BigDecimal totalPool, BigDecimal feeRate) {
    if (winningStakes.isEmpty()) {
      return List.of();
    }
    BigDecimal winningPool =
        winningStakes.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    if (winningPool.signum() <= 0 || totalPool.signum() < 0)
      throw new IllegalArgumentException("Pools must be valid");
    BigDecimal distributable = Money.amount(totalPool);
    List<Allocation> allocations =
        winningStakes.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(
                entry -> {
                  BigDecimal exact =
                      distributable
                          .multiply(entry.getValue())
                          .divide(winningPool, 12, Money.ROUNDING);
                  return new Allocation(
                      entry.getKey(), exact, exact.setScale(2, java.math.RoundingMode.DOWN));
                })
            .toList();

    BigDecimal allocated =
        allocations.stream().map(Allocation::gross).reduce(Money.ZERO, BigDecimal::add);
    int remainderCents = distributable.subtract(allocated).movePointRight(2).intValueExact();
    List<Allocation> remainderOrder =
        allocations.stream()
            .sorted(
                Comparator.comparing(
                        (Allocation allocation) -> allocation.exact().subtract(allocation.gross()))
                    .reversed()
                    .thenComparing(Allocation::betId, Comparator.reverseOrder()))
            .toList();
    Map<Long, BigDecimal> grossByBet = new HashMap<>();
    allocations.forEach(a -> grossByBet.put(a.betId(), a.gross()));
    for (int i = 0; i < remainderCents; i++) {
      Allocation allocation = remainderOrder.get(i);
      grossByBet.compute(allocation.betId(), (id, gross) -> gross.add(new BigDecimal("0.01")));
    }

    List<Share> result = new ArrayList<>();
    for (Allocation allocation : allocations) {
      BigDecimal stake = Money.amount(winningStakes.get(allocation.betId()));
      BigDecimal gross = grossByBet.get(allocation.betId());
      BigDecimal netWinnings = gross.subtract(stake).max(BigDecimal.ZERO);
      BigDecimal fee = Money.amount(netWinnings.multiply(feeRate));
      result.add(new Share(allocation.betId(), gross, fee, Money.amount(gross.subtract(fee))));
    }
    return List.copyOf(result);
  }
}
