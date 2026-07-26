package de.eva.forecastr.repository;

import de.eva.forecastr.core.models.LogEntry;
import de.eva.forecastr.core.models.LogType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
  List<LogEntry> findAllByOrderByTimestampDesc(Pageable pageable);

  List<LogEntry> findByTypeOrderByTimestampDesc(LogType type, Pageable pageable);
}
