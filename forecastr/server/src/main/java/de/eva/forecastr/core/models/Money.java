package de.eva.forecastr.core.models;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
  public static final int SCALE = 2;
  public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
  public static final BigDecimal ZERO = new BigDecimal("0.00");

  private Money() {}

  public static BigDecimal amount(BigDecimal value) {
    if (value == null) {
      throw new IllegalArgumentException("Money amount is required");
    }
    return value.setScale(SCALE, ROUNDING);
  }
}
