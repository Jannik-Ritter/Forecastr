package de.eva.forecastr.core.interfaces;

import de.eva.forecastr.core.models.ManualResolution;
import de.eva.forecastr.core.models.ResolutionResult;

public interface EventResolver {
  ResolutionResult resolve(Long eventId);

  ResolutionResult resolveManually(Long eventId, ManualResolution resolution, Long actorUserId);
}
