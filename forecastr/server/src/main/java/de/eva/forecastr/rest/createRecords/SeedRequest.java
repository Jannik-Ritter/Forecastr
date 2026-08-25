package de.eva.forecastr.rest.createRecords;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record SeedRequest(
    @Min(0) int users,
    @Min(0) int events,
    @Positive @Digits(integer = 17, fraction = 2) BigDecimal balance) {}
