package de.eva.forecastr.repository;

import de.eva.forecastr.core.models.MarketEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<MarketEvent, Long> {}
