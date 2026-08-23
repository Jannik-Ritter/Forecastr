package de.eva.forecastr.repository;

import de.eva.forecastr.core.models.EventStatus;

public record EventStatusCount(EventStatus status, long count) {}
