package de.eva.forecastr.clients.formatter;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class ConsoleFormatter {
  private static final Locale GERMAN = Locale.GERMANY;

  private ConsoleFormatter() {}

  public static String money(BigDecimal value) {
    NumberFormat format = NumberFormat.getCurrencyInstance(GERMAN);
    format.setMinimumFractionDigits(2);
    format.setMaximumFractionDigits(2);
    return format.format(value == null ? BigDecimal.ZERO : value);
  }

  public static void section(PrintStream output, String title) {
    output.println("\n" + title);
  }
}
