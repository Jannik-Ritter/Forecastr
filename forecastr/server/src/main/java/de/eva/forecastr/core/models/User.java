package de.eva.forecastr.core.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "app_user",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_username", columnNames = "username"))
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String username;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private boolean isAdmin;

  private Instant deletedAt;

  protected User() {}

  public User(String username, Instant createdAt, boolean isAdmin) {
    this.username = username;
    this.createdAt = createdAt;
    this.isAdmin = isAdmin;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public boolean isAdmin() {
    return isAdmin;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public void softDelete(Instant when) {
    deletedAt = when;
  }

  public boolean isDeleted() {
    return deletedAt != null;
  }
}
