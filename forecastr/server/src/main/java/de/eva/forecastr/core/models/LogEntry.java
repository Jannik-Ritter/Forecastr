package de.eva.forecastr.core.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import java.time.Instant;

@Entity
public class LogEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Instant timestamp;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private LogType type;

  @Lob
  @Column(nullable = false)
  private String payload;

  @Column(nullable = false, length = 120)
  private String threadName;

  protected LogEntry() {}

  public LogEntry(Instant timestamp, LogType type, String payload, String threadName) {
    this.timestamp = timestamp;
    this.type = type;
    this.payload = payload;
    this.threadName = threadName;
  }

  public Long getId() {
    return id;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public LogType getType() {
    return type;
  }

  public String getPayload() {
    return payload;
  }

  public String getThreadName() {
    return threadName;
  }
}
