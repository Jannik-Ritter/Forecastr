package de.eva.forecastr.rest.restComponents;

import java.time.Instant;

public record UserResponse(
    Long id, String username, boolean isAdmin, Instant createdAt, Instant deletedAt) {}
