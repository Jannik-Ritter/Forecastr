package de.eva.forecastr.rest.restComponents;

import de.eva.forecastr.core.models.Bet;
import de.eva.forecastr.core.models.EventStatus;
import de.eva.forecastr.core.services.BetService;
import de.eva.forecastr.core.services.EventService;
import de.eva.forecastr.rest.createRecords.BetRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EventController {
  private final EventService eventService;
  private final BetService betService;

  public EventController(EventService eventService, BetService betService) {
    this.eventService = eventService;
    this.betService = betService;
  }

  @GetMapping("/events")
  List<EventResponse> searchEvents(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) EventStatus status,
      @RequestParam(required = false) Instant endsBefore) {
    return eventService.searchEvents(name, status, endsBefore).stream()
        .map(RestMapper::event)
        .toList();
  }

  @GetMapping("/events/{id}")
  EventResponse getEvent(@PathVariable Long id) {
    return RestMapper.event(eventService.getEventById(id));
  }

  @GetMapping("/feed")
  List<EventResponse> getFeed(@RequestParam(defaultValue = "20") int limit) {
    return eventService.getFeed(limit).stream().map(RestMapper::event).toList();
  }

  @PostMapping("/events/{id}/bets")
  ResponseEntity<BetResponse> placeBet(
      @PathVariable Long id, @Valid @RequestBody BetRequest request) {
    Bet bet = betService.placeBet(id, request.userId(), request.outcome(), request.stake());
    return ResponseEntity.status(HttpStatus.CREATED).body(RestMapper.bet(bet));
  }
}
