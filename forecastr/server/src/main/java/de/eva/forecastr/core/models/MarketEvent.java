package de.eva.forecastr.core.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "market_event")
public class MarketEvent {
  @Id private Long id;

  @Column(nullable = false, length = 500)
  private String question;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant closesAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EventStatus status;

  private Instant resolvedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PlannedResolution plannedResolution;

  private Instant plannedResolutionAt;

  protected MarketEvent() {}

  public MarketEvent(
      Long id,
      String question,
      Instant createdAt,
      Instant closesAt,
      PlannedResolution plannedResolution,
      Instant plannedResolutionAt) {
    if (id == null) {
      throw new IllegalArgumentException("Event id is required");
    }
    if (question == null || !question.trim().endsWith("?"))
      throw new IllegalArgumentException("A binary question ending in '?' is required");
    if (createdAt == null || closesAt == null || !closesAt.isAfter(createdAt))
      throw new IllegalArgumentException("Event offsets are invalid");
    if (Duration.between(createdAt, closesAt).compareTo(Duration.ofHours(24)) > 0)
      throw new IllegalArgumentException("Event lifetime exceeds 24 hours");
    if (plannedResolution == null)
      throw new IllegalArgumentException("Planned resolution is required");
    if ((plannedResolution == PlannedResolution.UNRESOLVABLE) != (plannedResolutionAt == null))
      throw new IllegalArgumentException("Resolution time must be empty iff unresolvable");
    if (plannedResolutionAt != null
        && (plannedResolutionAt.isBefore(createdAt) || plannedResolutionAt.isAfter(closesAt)))
      throw new IllegalArgumentException("Resolution time must be within the event lifetime");
    this.id = id;
    this.question = question;
    this.createdAt = createdAt;
    this.closesAt = closesAt;
    this.status = EventStatus.OPEN;
    this.plannedResolution = plannedResolution;
    this.plannedResolutionAt = plannedResolutionAt;
  }

  public Long getId() {
    return id;
  }

  public String getQuestion() {
    return question;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getClosesAt() {
    return closesAt;
  }

  public EventStatus getStatus() {
    return status;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public PlannedResolution getPlannedResolution() {
    return plannedResolution;
  }

  public Instant getPlannedResolutionAt() {
    return plannedResolutionAt;
  }

  public void resolve(Outcome outcome, Instant when) {
    status = outcome == Outcome.YES ? EventStatus.RESOLVED_YES : EventStatus.RESOLVED_NO;
    resolvedAt = when;
  }

  public void expire(Instant when) {
    status = EventStatus.EXPIRED;
    resolvedAt = when;
  }

  public void archive() {
    status = EventStatus.ARCHIVED;
  }
}
