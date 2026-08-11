package de.eva.forecastr.core.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "fee_revenue",
    uniqueConstraints = @UniqueConstraint(name = "uk_fee_revenue_bet", columnNames = "betId"))
public class FeeRevenue {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long betId;

  @Column(nullable = false)
  private Long eventId;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false)
  private Instant bookedAt;

  protected FeeRevenue() {}

  public FeeRevenue(Long betId, Long eventId, BigDecimal amount, Instant bookedAt) {
    if (betId == null || eventId == null || bookedAt == null)
      throw new IllegalArgumentException("Fee booking references and timestamp are required");
    amount = Money.amount(amount);
    if (amount.signum() <= 0)
      throw new IllegalArgumentException("Fee booking amount must be positive");
    this.betId = betId;
    this.eventId = eventId;
    this.amount = amount;
    this.bookedAt = bookedAt;
  }

  public Long getId() {
    return id;
  }

  public Long getBetId() {
    return betId;
  }

  public Long getEventId() {
    return eventId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Instant getBookedAt() {
    return bookedAt;
  }
}
