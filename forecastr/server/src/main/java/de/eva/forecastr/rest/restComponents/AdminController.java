package de.eva.forecastr.rest.restComponents;

import de.eva.forecastr.config.AdminAuthorizationInterceptor;
import de.eva.forecastr.core.application.AdminFacade;
import de.eva.forecastr.core.models.ImportReport;
import de.eva.forecastr.core.models.ResolutionResult;
import de.eva.forecastr.core.models.SeedResult;
import de.eva.forecastr.core.models.TestEventsResult;
import de.eva.forecastr.core.models.TestUsersResult;
import de.eva.forecastr.rest.createRecords.ManualResolutionRequest;
import de.eva.forecastr.rest.createRecords.SeedRequest;
import de.eva.forecastr.rest.createRecords.TestEventsRequest;
import de.eva.forecastr.rest.createRecords.TestUsersRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {
  private final AdminFacade adminFacade;

  public AdminController(AdminFacade adminFacade) {
    this.adminFacade = adminFacade;
  }

  @PostMapping("/import")
  ImportReport importEvents(
      @RequestHeader(AdminAuthorizationInterceptor.ACTOR_HEADER) Long actorUserId,
      @RequestParam(required = false) String path) {
    return adminFacade.importEvents(actorUserId, path);
  }

  @PostMapping("/seed")
  SeedResult seed(
      @RequestHeader(AdminAuthorizationInterceptor.ACTOR_HEADER) Long actorUserId,
      @Valid @RequestBody SeedRequest request) {
    return adminFacade.seed(actorUserId, request.users(), request.events(), request.balance());
  }

  @PostMapping("/test-data/users")
  TestUsersResult seedTestUsers(
      @RequestHeader(AdminAuthorizationInterceptor.ACTOR_HEADER) Long actorUserId,
      @Valid @RequestBody TestUsersRequest request) {
    return adminFacade.seedTestUsers(
        actorUserId,
        request.count(),
        request.betsPerUser(),
        request.eventId(),
        request.outcome(),
        request.stake());
  }

  @PostMapping("/test-data/events")
  TestEventsResult seedTestEvents(
      @RequestHeader(AdminAuthorizationInterceptor.ACTOR_HEADER) Long actorUserId,
      @Valid @RequestBody TestEventsRequest request) {
    return adminFacade.seedTestEvents(actorUserId, request.count(), request.expiresInMinutes());
  }

  @PostMapping("/events/{id}/resolve")
  ResolutionResult resolveEvent(
      @RequestHeader(AdminAuthorizationInterceptor.ACTOR_HEADER) Long actorUserId,
      @PathVariable Long id,
      @Valid @RequestBody ManualResolutionRequest request) {
    return adminFacade.resolveEvent(actorUserId, id, request.outcome());
  }
}
