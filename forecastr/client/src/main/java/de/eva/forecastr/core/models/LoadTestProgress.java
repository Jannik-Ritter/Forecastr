package de.eva.forecastr.core.models;

public record LoadTestProgress(
    int completed, int total, String step, Status status, String failureDetail) {
  public LoadTestProgress {
    if (total < 1 || completed < 0 || completed > total) {
      throw new IllegalArgumentException("Invalid load-test progress");
    }
    if (step == null || step.isBlank()) {
      throw new IllegalArgumentException("Load-test step is required");
    }
    if (status == null) {
      throw new IllegalArgumentException("Load-test status is required");
    }
  }

  public static LoadTestProgress running(int completed, int total, String step) {
    return new LoadTestProgress(completed, total, step, Status.RUNNING, null);
  }

  public static LoadTestProgress completed(
      int completed, int total, String step, boolean successful) {
    return completed(completed, total, step, successful, null);
  }

  public static LoadTestProgress completed(
      int completed, int total, String step, boolean successful, String failureDetail) {
    return new LoadTestProgress(
        completed,
        total,
        step,
        successful ? Status.PASSED : Status.FAILED,
        successful ? null : failureDetail);
  }

  public enum Status {
    RUNNING,
    PASSED,
    FAILED
  }
}
