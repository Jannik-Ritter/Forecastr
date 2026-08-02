package de.eva.forecastr.core.models;

import java.time.Instant;

public record User(
    long id, String username, boolean isAdmin, Instant createdAt, Instant deletedAt) {}
