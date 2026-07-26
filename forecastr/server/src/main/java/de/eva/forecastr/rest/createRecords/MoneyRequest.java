package de.eva.forecastr.rest.createRecords;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record MoneyRequest(
    @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount) {}
