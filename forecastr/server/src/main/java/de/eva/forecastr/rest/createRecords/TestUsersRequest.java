package de.eva.forecastr.rest.createRecords;

import de.eva.forecastr.core.models.Outcome;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TestUsersRequest(
    @Min(1) int count,
    @Min(0) int betsPerUser,
    @Positive Long eventId,
    Outcome outcome,
    @Positive @Digits(integer = 17, fraction = 2) BigDecimal stake) {}
