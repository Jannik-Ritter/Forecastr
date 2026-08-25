package de.eva.forecastr.clients.load;

public record StressMeasurement(int index, int status, long latencyNanos, String operation) {}
