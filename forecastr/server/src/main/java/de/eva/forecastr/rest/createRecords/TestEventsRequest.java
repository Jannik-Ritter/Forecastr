package de.eva.forecastr.rest.createRecords;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record TestEventsRequest(@Min(1) int count, @Min(1) @Max(1440) Integer expiresInMinutes) {}
