package de.eva.forecastr.rest.restComponents;

import de.eva.forecastr.core.models.LogType;
import de.eva.forecastr.core.models.StatsSnapshot;
import de.eva.forecastr.core.services.LogService;
import de.eva.forecastr.core.services.StatsService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OperationsController {
  private final StatsService statsService;
  private final LogService logService;

  public OperationsController(StatsService statsService, LogService logService) {
    this.statsService = statsService;
    this.logService = logService;
  }

  @GetMapping("/stats")
  StatsSnapshot getStats() {
    return statsService.getStats();
  }

  @GetMapping("/logs")
  List<LogEntryResponse> getLogs(
      @RequestParam(required = false) LogType type, @RequestParam(defaultValue = "100") int limit) {
    return logService.getLogs(type, limit).stream().map(RestMapper::logEntry).toList();
  }
}
