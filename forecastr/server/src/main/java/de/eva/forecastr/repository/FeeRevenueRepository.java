package de.eva.forecastr.repository;

import de.eva.forecastr.core.models.FeeRevenue;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FeeRevenueRepository extends JpaRepository<FeeRevenue, Long> {
  @Query("select coalesce(sum(f.amount),0) from FeeRevenue f")
  BigDecimal sumAmount();
}
