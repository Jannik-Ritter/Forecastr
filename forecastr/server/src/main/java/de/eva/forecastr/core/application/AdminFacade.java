package de.eva.forecastr.core.application;

import de.eva.forecastr.core.interfaces.EventSource;
import de.eva.forecastr.core.models.ImportReport;
import de.eva.forecastr.core.models.LogType;
import de.eva.forecastr.core.services.LogService;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminFacade {
  private final EventSource eventSource;
  private final LogService logService;

  public AdminFacade(
      EventSource eventSource, LogService logService) {
    this.eventSource = eventSource;
    this.logService = logService;
  }

  @Transactional
  public ImportReport importEvents(Long actorUserId, String path) {
    boolean useDefaults = path == null || path.isBlank();
    ImportReport report =
        useDefaults ? eventSource.importDefaults() : eventSource.importPath(Path.of(path));
    logService.log(
        LogType.ADMIN,
        Map.of(
            "actorUserId",
            actorUserId,
            "action",
            "IMPORT",
            "source",
            useDefaults ? "DEFAULTS" : path,
            "accepted",
            report.accepted(),
            "rejected",
            report.rejected(),
            "skipped",
            report.skipped()));
    return report;
  }

}
