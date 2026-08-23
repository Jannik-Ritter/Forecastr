package de.eva.forecastr.rest.restComponents;

import de.eva.forecastr.core.models.StatsSnapshot;
import de.eva.forecastr.core.services.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OperationsController {
  private final StatsService statsService;

  public OperationsController(StatsService statsService) {
    this.statsService = statsService;
  }

  @GetMapping("/stats")
  StatsSnapshot getStats() {
    return statsService.getStats();
  }

}
