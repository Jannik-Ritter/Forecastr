package de.eva.forecastr.rest.createRecords;

import de.eva.forecastr.core.models.ManualResolution;
import jakarta.validation.constraints.NotNull;

public record ManualResolutionRequest(@NotNull ManualResolution outcome) {}
