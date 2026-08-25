package de.eva.forecastr.clients.load;

import de.eva.forecastr.rest.RestResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

public final class StressExecutor {
  public StressReport execute(
      String name,
      int requestCount,
      int concurrency,
      IntFunction<RestResponse> action,
      List<StressMeasurement> sink) {
    if (requestCount < 1 || concurrency < 1) {
      throw new IllegalArgumentException("-n and -c must be positive");
    }
    List<StressMeasurement> measurements = Collections.synchronizedList(sink);
    CountDownLatch ready = new CountDownLatch(requestCount);
    CountDownLatch start = new CountDownLatch(1);
    Semaphore slots = new Semaphore(Math.min(requestCount, concurrency));
    long wallStartedAt;
    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      List<Future<?>> futures =
          submit(executor, requestCount, name, action, measurements, ready, start, slots);
      awaitWorkers(ready);
      wallStartedAt = System.nanoTime();
      start.countDown();
      awaitResults(futures);
    }
    return report(name, measurements, System.nanoTime() - wallStartedAt);
  }

  public StressReport report(String name, List<StressMeasurement> measurements, long wallNanos) {
    long successes =
        measurements.stream()
            .filter(value -> value.status() >= 200 && value.status() < 300)
            .count();
    long clientErrors =
        measurements.stream()
            .filter(value -> value.status() >= 400 && value.status() < 500)
            .count();
    long serverErrors = measurements.stream().filter(value -> value.status() >= 500).count();
    long timeouts = measurements.stream().filter(value -> value.status() <= 0).count();
    return new StressReport(
        name,
        measurements.size(),
        wallNanos,
        successes,
        clientErrors,
        serverErrors,
        timeouts,
        measurements.stream().mapToLong(StressMeasurement::latencyNanos).toArray());
  }

  private List<Future<?>> submit(
      ExecutorService executor,
      int requestCount,
      String name,
      IntFunction<RestResponse> action,
      List<StressMeasurement> measurements,
      CountDownLatch ready,
      CountDownLatch start,
      Semaphore slots) {
    List<Future<?>> futures = new ArrayList<>(requestCount);
    for (int index = 0; index < requestCount; index++) {
      int requestIndex = index;
      futures.add(
          executor.submit(
              () -> executeRequest(requestIndex, name, action, measurements, ready, start, slots)));
    }
    return futures;
  }

  private void executeRequest(
      int index,
      String name,
      IntFunction<RestResponse> action,
      List<StressMeasurement> measurements,
      CountDownLatch ready,
      CountDownLatch start,
      Semaphore slots) {
    ready.countDown();
    try {
      start.await();
      slots.acquire();
      try {
        RestResponse response = action.apply(index);
        measurements.add(
            new StressMeasurement(index, response.status(), response.latencyNanos(), name));
      } finally {
        slots.release();
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      measurements.add(new StressMeasurement(index, 0, 0, name));
    } catch (RuntimeException exception) {
      measurements.add(new StressMeasurement(index, 500, 0, name));
    }
  }

  private void awaitWorkers(CountDownLatch ready) {
    try {
      if (!ready.await(30, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Virtual workers did not reach barrier");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while starting virtual workers", exception);
    }
  }

  private void awaitResults(List<Future<?>> futures) {
    for (Future<?> future : futures) {
      try {
        future.get();
      } catch (Exception exception) {
        throw new IllegalStateException("Stress worker failed", exception);
      }
    }
  }
}
