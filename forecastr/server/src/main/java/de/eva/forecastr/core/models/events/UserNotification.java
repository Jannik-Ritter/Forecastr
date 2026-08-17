package de.eva.forecastr.core.models.events;

import java.math.BigDecimal;

public record UserNotification(Long userId, Long eventId, String kind, BigDecimal amount) {}
