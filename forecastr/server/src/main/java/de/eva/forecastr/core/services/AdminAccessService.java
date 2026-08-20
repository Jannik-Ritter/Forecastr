package de.eva.forecastr.core.services;

import de.eva.forecastr.core.models.User;
import de.eva.forecastr.core.models.exceptions.ForecastrException;
import de.eva.forecastr.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccessService {
  private final UserRepository userRepository;

  public AdminAccessService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public void requireAdmin(long actorUserId) {
    User actor =
        userRepository
            .findById(actorUserId)
            .orElseThrow(() -> ForecastrException.forbidden("Admin account required"));
    if (actor.isDeleted() || !actor.isAdmin()) {
      throw ForecastrException.forbidden("Admin account required");
    }
  }
}
