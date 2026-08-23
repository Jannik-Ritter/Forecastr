package de.eva.forecastr.config;

import de.eva.forecastr.core.models.EventStatus;
import de.eva.forecastr.core.models.events.ImportsRejected;
import de.eva.forecastr.core.models.events.ResolutionRecorded;
import de.eva.forecastr.core.services.PlatformMetrics;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MetricsEventHandler {
  private final PlatformMetrics platformMetrics;

  public MetricsEventHandler(PlatformMetrics platformMetrics) {
    this.platformMetrics = platformMetrics;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void recordRejectedImports(ImportsRejected event) {
    platformMetrics.rejectedImports(event.count());
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void recordResolution(ResolutionRecorded event) {
    if (event.status() == EventStatus.EXPIRED) {
      platformMetrics.expired();
    } else {
      platformMetrics.resolved();
    }
  }
}
