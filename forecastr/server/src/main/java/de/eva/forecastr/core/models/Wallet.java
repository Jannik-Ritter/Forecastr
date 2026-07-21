package de.eva.forecastr.core.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import java.math.BigDecimal;

@Entity
public class Wallet {
  @Id private Long userId;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal balance;

  @Version private long version;

  protected Wallet() {}

  public Wallet(Long userId, BigDecimal balance) {
    this.userId = userId;
    this.balance = Money.amount(balance);
  }

  public Long getUserId() {
    return userId;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public long getVersion() {
    return version;
  }

  public void credit(BigDecimal amount) {
    balance = Money.amount(balance.add(Money.amount(amount)));
  }

  public void debit(BigDecimal amount) {
    amount = Money.amount(amount);
    if (amount.signum() <= 0) {
      throw new IllegalArgumentException("Amount must be positive");
    }
    if (balance.compareTo(amount) < 0) {
      throw new IllegalStateException("Insufficient balance");
    }
    balance = Money.amount(balance.subtract(amount));
  }
}
