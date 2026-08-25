package de.eva.forecastr.rest.restComponents;

import de.eva.forecastr.core.services.BetService;
import de.eva.forecastr.core.services.EventService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("test")
@RestController
@RequestMapping("/admin/events")
public class TestResolutionController {
  private final BetService betService;
  private final EventService eventService;

  public TestResolutionController(BetService betService, EventService eventService) {
    this.betService = betService;
    this.eventService = eventService;
  }

  @GetMapping("/{id}/audit")
  ResolutionAuditResponse audit(@PathVariable Long id) {
    return new ResolutionAuditResponse(
        RestMapper.event(eventService.getEventById(id)),
        betService.getBetsByEvent(id).stream().map(RestMapper::bet).toList());
  }
}
