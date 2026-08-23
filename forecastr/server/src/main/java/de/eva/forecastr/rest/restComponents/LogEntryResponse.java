package de.eva.forecastr.rest.restComponents;

import de.eva.forecastr.core.models.LogType;
import java.time.Instant;

public record LogEntryResponse(
    Long id, Instant timestamp, LogType type, String payload, String threadName) {}
