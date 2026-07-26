package de.eva.forecastr.rest.createRecords;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateUserRequest(
    @NotBlank @Size(max = 80) String username,
    @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal initialBalance) {}
