package de.eva.forecastr.core.models.exceptions;

public final class ForecastrException extends RuntimeException {
  private final FailureKind kind;

  private ForecastrException(FailureKind kind, String message) {
    super(message);
    this.kind = kind;
  }

  public FailureKind kind() {
    return kind;
  }

  public static ForecastrException notFound(String message) {
    return new ForecastrException(FailureKind.NOT_FOUND, message);
  }

  public static ForecastrException conflict(String message) {
    return new ForecastrException(FailureKind.CONFLICT, message);
  }

  public static ForecastrException forbidden(String message) {
    return new ForecastrException(FailureKind.FORBIDDEN, message);
  }

  public static ForecastrException paymentRequired(String message) {
    return new ForecastrException(FailureKind.PAYMENT_REQUIRED, message);
  }
}
