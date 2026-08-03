package de.eva.forecastr.rest.createRecords;

import de.eva.forecastr.core.models.Outcome;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record BetRequest(
    @NotNull Long userId,
    @NotNull Outcome outcome,
    @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal stake) {}
