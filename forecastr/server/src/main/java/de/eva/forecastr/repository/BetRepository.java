package de.eva.forecastr.repository;

import de.eva.forecastr.core.models.Bet;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BetRepository extends JpaRepository<Bet, Long> {
  List<Bet> findByUserIdOrderByPlacedAtDesc(Long userId);

  void deleteByUserId(Long userId);

  List<Bet> findByEventIdOrderById(Long eventId);

  @Query("select distinct b.eventId from Bet b where b.userId = :userId order by b.eventId")
  List<Long> findEventIdsByUserId(@Param("userId") Long userId);

  @Query(
      "select new de.eva.forecastr.repository.EventPoolTotal(b.eventId, b.outcome,"
          + " sum(b.stake)) from Bet b where b.eventId in :eventIds group by b.eventId,"
          + " b.outcome")
  List<EventPoolTotal> sumPools(@Param("eventIds") Collection<Long> eventIds);

  @Query(
      "select new de.eva.forecastr.repository.UserBetCount(b.userId, count(b))"
          + " from Bet b group by b.userId order by b.userId")
  List<UserBetCount> countByUser();

  @Query("select coalesce(sum(b.payoutAmount),0) from Bet b")
  BigDecimal sumPayouts();
}
