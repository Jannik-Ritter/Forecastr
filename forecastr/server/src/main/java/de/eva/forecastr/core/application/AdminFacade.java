package de.eva.forecastr.core.application;

import de.eva.forecastr.core.interfaces.EventResolver;
import de.eva.forecastr.core.interfaces.EventSource;
import de.eva.forecastr.core.models.ImportReport;
import de.eva.forecastr.core.models.LogType;
import de.eva.forecastr.core.models.ManualResolution;
import de.eva.forecastr.core.models.Outcome;
import de.eva.forecastr.core.models.ResolutionResult;
import de.eva.forecastr.core.models.SeedResult;
import de.eva.forecastr.core.models.TestEventsResult;
import de.eva.forecastr.core.models.TestUsersResult;
import de.eva.forecastr.core.services.LogService;
import de.eva.forecastr.core.services.SeedService;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminFacade {
  private final EventSource eventSource;
  private final SeedService seedService;
  private final EventResolver eventResolver;
  private final LogService logService;

  public AdminFacade(
      EventSource eventSource,
      SeedService seedService,
      EventResolver eventResolver,
      LogService logService) {
    this.eventSource = eventSource;
    this.seedService = seedService;
    this.eventResolver = eventResolver;
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

  @Transactional
  public SeedResult seed(Long actorUserId, int users, int events, BigDecimal balance) {
    SeedResult result = seedService.seed(users, events, balance);
    logService.log(
        LogType.ADMIN,
        Map.of(
            "actorUserId", actorUserId,
            "action", "SEED",
            "users", users,
            "events", events));
    return result;
  }

  @Transactional
  public TestUsersResult seedTestUsers(
      Long actorUserId,
      int count,
      int betsPerUser,
      Long eventId,
      Outcome outcome,
      BigDecimal stake) {
    TestUsersResult result = seedService.seedTestUsers(count, betsPerUser, eventId, outcome, stake);
    logService.log(
        LogType.ADMIN,
        Map.of(
            "actorUserId", actorUserId,
            "action", "SEED_TEST_USERS",
            "users", result.userIds().size(),
            "bets", result.betIds().size(),
            "event", Objects.toString(eventId, "RANDOM"),
            "outcome", Objects.toString(outcome, "RANDOM"),
            "stake", Objects.toString(stake, "RANDOM")));
    return result;
  }

  @Transactional
  public TestEventsResult seedTestEvents(Long actorUserId, int count, Integer expiresInMinutes) {
    TestEventsResult result = seedService.seedTestEvents(count, expiresInMinutes);
    logService.log(
        LogType.ADMIN,
        Map.of(
            "actorUserId",
            actorUserId,
            "action",
            "SEED_TEST_EVENTS",
            "events",
            result.eventIds().size(),
            "expiresInMinutes",
            expiresInMinutes == null ? 10 : expiresInMinutes));
    return result;
  }

  @Transactional
  public ResolutionResult resolveEvent(
      Long actorUserId, Long eventId, ManualResolution resolution) {
    return eventResolver.resolveManually(eventId, resolution, actorUserId);
  }
}
