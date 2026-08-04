package de.eva.forecastr.core.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    indexes = {
      @Index(name = "idx_bet_event", columnList = "eventId"),
      @Index(name = "idx_bet_user", columnList = "userId")
    })
public class Bet {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private Long eventId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Outcome outcome;

  @Column(nullable = false, precision = 19, scale = 2)
  private BigDecimal stake;

  @Column(nullable = false)
  private Instant placedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BetStatus status;

  @Column(precision = 19, scale = 2)
  private BigDecimal payoutAmount;

  @Column(precision = 19, scale = 2)
  private BigDecimal feeAmount;

  protected Bet() {}

  public Bet(Long userId, Long eventId, Outcome outcome, BigDecimal stake, Instant placedAt) {
    this.userId = userId;
    this.eventId = eventId;
    this.outcome = outcome;
    this.stake = Money.amount(stake);
    this.placedAt = placedAt;
    this.status = BetStatus.OPEN;
    this.payoutAmount = Money.ZERO;
    this.feeAmount = Money.ZERO;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getEventId() {
    return eventId;
  }

  public Outcome getOutcome() {
    return outcome;
  }

  public BigDecimal getStake() {
    return stake;
  }

  public Instant getPlacedAt() {
    return placedAt;
  }

  public BetStatus getStatus() {
    return status;
  }

  public BigDecimal getPayoutAmount() {
    return payoutAmount;
  }

  public BigDecimal getFeeAmount() {
    return feeAmount;
  }

  public void win(BigDecimal payout, BigDecimal fee) {
    status = BetStatus.WON;
    payoutAmount = Money.amount(payout);
    feeAmount = Money.amount(fee);
  }

  public void lose() {
    status = BetStatus.LOST;
    payoutAmount = Money.ZERO;
    feeAmount = Money.ZERO;
  }

  public void refund() {
    status = BetStatus.REFUNDED;
    payoutAmount = stake;
    feeAmount = Money.ZERO;
  }
}
