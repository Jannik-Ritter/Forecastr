package de.eva.forecastr.clients.load;

import java.util.Arrays;
import java.util.Locale;

public record StressReport(
    String command,
    int total,
    long wallNanos,
    long successes,
    long clientErrors,
    long serverErrors,
    long timeouts,
    long[] latencies) {
  public StressReport {
    latencies = latencies.clone();
  }

  @Override
  public long[] latencies() {
    return latencies.clone();
  }

  public double errorRate() {
    return total == 0 ? 0 : (total - successes) * 100d / total;
  }

  public String summary() {
    return String.format(
        Locale.ROOT,
        "%s: total=%d wall=%.3fs rate=%.1f req/s success=%d 4xx=%d 5xx=%d timeout=%d"
            + " latency ms min=%.2f mean=%.2f p50=%.2f p95=%.2f p99=%.2f max=%.2f",
        command,
        total,
        wallNanos / 1e9,
        requestsPerSecond(),
        successes,
        clientErrors,
        serverErrors,
        timeouts,
        milliseconds(percentile(0)),
        meanMilliseconds(),
        milliseconds(percentile(.50)),
        milliseconds(percentile(.95)),
        milliseconds(percentile(.99)),
        milliseconds(percentile(1)));
  }

  private double requestsPerSecond() {
    return total / (wallNanos / 1_000_000_000d);
  }

  private double milliseconds(long nanos) {
    return nanos / 1_000_000d;
  }

  private long percentile(double percentile) {
    if (latencies.length == 0) {
      return 0;
    }
    long[] sorted = latencies.clone();
    Arrays.sort(sorted);
    int index = Math.max(0, (int) Math.ceil(percentile * sorted.length) - 1);
    return sorted[Math.min(sorted.length - 1, index)];
  }

  private double meanMilliseconds() {
    return Arrays.stream(latencies).average().orElse(0) / 1_000_000d;
  }
}
