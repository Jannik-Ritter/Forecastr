package de.eva.forecastr.rest;

public record RestResponse(int status, String body, long latencyNanos) {
  public boolean isSuccess() {
    return status >= 200 && status < 300;
  }
}
