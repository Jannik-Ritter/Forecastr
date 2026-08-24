package de.eva.forecastr.rest.restComponents;

import de.eva.forecastr.config.AdminAuthorizationInterceptor;
import de.eva.forecastr.core.application.AdminFacade;
import de.eva.forecastr.core.models.ImportReport;
import de.eva.forecastr.core.models.ResolutionResult;
import de.eva.forecastr.rest.createRecords.ManualResolutionRequest;
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

  @PostMapping("/events/{id}/resolve")
  ResolutionResult resolveEvent(
      @RequestHeader(AdminAuthorizationInterceptor.ACTOR_HEADER) Long actorUserId,
      @PathVariable Long id,
      @Valid @RequestBody ManualResolutionRequest request) {
    return adminFacade.resolveEvent(actorUserId, id, request.outcome());
  }
}
