package de.eva.forecastr.repository;

import de.eva.forecastr.core.models.Bet;
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

}
