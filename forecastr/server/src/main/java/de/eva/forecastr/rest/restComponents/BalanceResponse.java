package de.eva.forecastr.rest.restComponents;

import java.math.BigDecimal;

public record BalanceResponse(Long userId, BigDecimal balance, String currency, long version) {}
