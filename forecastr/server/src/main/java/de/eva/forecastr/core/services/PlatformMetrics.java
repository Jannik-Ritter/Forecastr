package de.eva.forecastr.core.services;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class PlatformMetrics {
  private final AtomicLong resolved = new AtomicLong();
  private final AtomicLong expired = new AtomicLong();
  private final AtomicLong rejectedImports = new AtomicLong();

  public void resolved() {
    resolved.incrementAndGet();
  }

  public void expired() {
    expired.incrementAndGet();
  }

  public void rejectedImports(long count) {
    rejectedImports.addAndGet(count);
  }

  public long resolvedCount() {
    return resolved.get();
  }

  public long expiredCount() {
    return expired.get();
  }

  public long rejectedImportCount() {
    return rejectedImports.get();
  }
}
