package de.eva.forecastr.repository;

import de.eva.forecastr.core.models.MarketEvent;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository
    extends JpaRepository<MarketEvent, Long>, JpaSpecificationExecutor<MarketEvent> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from MarketEvent e where e.id = :id")
  Optional<MarketEvent> findLocked(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from MarketEvent e where e.id in :ids order by e.id")
  List<MarketEvent> findAllLocked(@Param("ids") Collection<Long> ids);

  @Query(
      "select e from MarketEvent e where e.status ="
          + " de.eva.forecastr.core.models.EventStatus.OPEN and e.createdAt <= :now order by"
          + " e.createdAt desc, e.closesAt asc")
  List<MarketEvent> findFeed(@Param("now") Instant now, Pageable pageable);

  @Query(
      "select e from MarketEvent e where e.status ="
          + " de.eva.forecastr.core.models.EventStatus.OPEN and e.createdAt <= :now and"
          + " e.closesAt > :now order by e.id")
  List<MarketEvent> findBettable(@Param("now") Instant now);

  @Query(
      "select e.id from MarketEvent e where e.status ="
          + " de.eva.forecastr.core.models.EventStatus.OPEN and ((e.plannedResolution <>"
          + " de.eva.forecastr.core.models.PlannedResolution.UNRESOLVABLE and"
          + " e.plannedResolutionAt <= :now) or e.closesAt <= :now)")
  List<Long> findDueIds(@Param("now") Instant now);

  @Query(
      "select e.id from MarketEvent e where e.status in"
          + " (de.eva.forecastr.core.models.EventStatus.RESOLVED_YES,"
          + " de.eva.forecastr.core.models.EventStatus.RESOLVED_NO,"
          + " de.eva.forecastr.core.models.EventStatus.EXPIRED)"
          + " and e.resolvedAt <= :cutoff")
  List<Long> findArchivableIds(@Param("cutoff") Instant cutoff);

  @Query(
      "select new de.eva.forecastr.repository.EventStatusCount(e.status, count(e))"
          + " from MarketEvent e group by e.status")
  List<EventStatusCount> countByStatus();
}
