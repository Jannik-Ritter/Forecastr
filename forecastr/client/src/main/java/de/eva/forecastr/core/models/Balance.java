package de.eva.forecastr.core.models;

import java.math.BigDecimal;

public record Balance(long userId, BigDecimal balance, String currency, long version) {}
