package de.eva.forecastr.repository;

import de.eva.forecastr.core.models.MarketEvent;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<MarketEvent, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from MarketEvent e where e.id = :id")
  Optional<MarketEvent> findLocked(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from MarketEvent e where e.id in :ids order by e.id")
  List<MarketEvent> findAllLocked(@Param("ids") Collection<Long> ids);
}
