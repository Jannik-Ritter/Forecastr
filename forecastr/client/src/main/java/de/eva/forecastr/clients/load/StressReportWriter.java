package de.eva.forecastr.clients.load;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class StressReportWriter {
  public void write(Path output, List<StressMeasurement> measurements) {
    if (output == null) {
      return;
    }
    boolean writeHeader = !Files.exists(output);
    try (BufferedWriter writer =
        Files.newBufferedWriter(
            output, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      if (writeHeader) {
        writer.write("command,index,status,latencyNanos\n");
      }
      for (StressMeasurement measurement : measurements) {
        writer.write(
            measurement.operation()
                + ","
                + measurement.index()
                + ","
                + measurement.status()
                + ","
                + measurement.latencyNanos()
                + "\n");
      }
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
