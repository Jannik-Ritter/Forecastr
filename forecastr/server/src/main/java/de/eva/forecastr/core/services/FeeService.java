package de.eva.forecastr.core.services;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FeeService {
  private final BigDecimal rate;

  public FeeService(@Value("${forecastr.fee-rate:0.05}") BigDecimal rate) {
    if (rate == null || rate.signum() < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
      throw new IllegalArgumentException("Fee rate must be between 0 and 1");
    }
    this.rate = rate;
  }

  public BigDecimal rate() {
    return rate;
  }
}
