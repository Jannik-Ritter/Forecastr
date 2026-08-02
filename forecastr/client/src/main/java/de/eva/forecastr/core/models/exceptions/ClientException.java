package de.eva.forecastr.core.models.exceptions;

public final class ClientException extends RuntimeException {
  private final int status;

  public ClientException(int status, String message) {
    super(message);
    this.status = status;
  }

  public int status() {
    return status;
  }
}
