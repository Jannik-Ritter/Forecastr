package de.eva.forecastr.core.models;

import java.util.List;

public record ImportReport(int accepted, int rejected, int skipped, List<String> errors) {
  public ImportReport {
    errors = List.copyOf(errors);
  }
}
