package de.eva.forecastr.repository;

import de.eva.forecastr.core.models.Outcome;
import java.math.BigDecimal;

public record EventPoolTotal(Long eventId, Outcome outcome, BigDecimal total) {}
