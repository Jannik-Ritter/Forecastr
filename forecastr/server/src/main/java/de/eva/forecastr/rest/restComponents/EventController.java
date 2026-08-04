package de.eva.forecastr.rest.restComponents;

import de.eva.forecastr.core.models.Bet;
import de.eva.forecastr.core.services.BetService;
import de.eva.forecastr.rest.createRecords.BetRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
public class EventController {
  private final BetService betService;

  public EventController(BetService betService) {
    this.betService = betService;
  }

  @PostMapping("/{id}/bets")
  ResponseEntity<BetResponse> placeBet(
      @PathVariable Long id, @Valid @RequestBody BetRequest request) {
    Bet bet = betService.placeBet(id, request.userId(), request.outcome(), request.stake());
    return ResponseEntity.status(HttpStatus.CREATED).body(RestMapper.bet(bet));
  }
}
