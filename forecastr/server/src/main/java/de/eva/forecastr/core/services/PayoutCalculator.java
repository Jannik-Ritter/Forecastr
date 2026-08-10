package de.eva.forecastr.core.services;

import de.eva.forecastr.core.models.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PayoutCalculator {
  private PayoutCalculator() {}

  public record Share(long betId, BigDecimal gross, BigDecimal fee, BigDecimal credited) {}

  public static List<Share> calculate(
      Map<Long, BigDecimal> winningStakes, BigDecimal totalPool, BigDecimal feeRate) {
    if (winningStakes.isEmpty()) {
      return List.of();
    }
    BigDecimal winningPool =
        winningStakes.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    if (winningPool.signum() <= 0 || totalPool.signum() < 0) {
      throw new IllegalArgumentException("Pools must be valid");
    }
    List<Share> result = new ArrayList<>();
    for (Map.Entry<Long, BigDecimal> entry : winningStakes.entrySet()) {
      BigDecimal stake = Money.amount(entry.getValue());
      BigDecimal gross =
          Money.amount(Money.amount(totalPool).multiply(stake).divide(winningPool, 12, Money.ROUNDING));
      BigDecimal netWinnings = gross.subtract(stake).max(BigDecimal.ZERO);
      BigDecimal fee = Money.amount(netWinnings.multiply(feeRate));
      result.add(new Share(entry.getKey(), gross, fee, Money.amount(gross.subtract(fee))));
    }
    return List.copyOf(result);
  }
}
