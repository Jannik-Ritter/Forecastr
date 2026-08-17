package de.eva.forecastr.clients.formatter;

import de.eva.forecastr.core.models.Bet;
import de.eva.forecastr.core.models.LiveUpdate;
import de.eva.forecastr.core.models.Market;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ConsoleFormatter {
  private static final Locale GERMAN = Locale.GERMANY;
  private static final DateTimeFormatter DATE =
      DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(GERMAN);

  private ConsoleFormatter() {}

  public static String money(BigDecimal value) {
    NumberFormat format = NumberFormat.getCurrencyInstance(GERMAN);
    format.setMinimumFractionDigits(2);
    format.setMaximumFractionDigits(2);
    return format.format(value == null ? BigDecimal.ZERO : value);
  }

  public static String date(Instant value) {
    if (value == null) {
      return "–";
    }
    return DATE.format(value.atZone(ZoneId.systemDefault()));
  }

  public static String deadline(Instant closesAt, Instant now) {
    if (closesAt == null) {
      return "unbekannt";
    }
    Duration remaining = Duration.between(now, closesAt);
    if (remaining.isNegative() || remaining.isZero()) {
      return "geschlossen";
    }
    long minutes = remaining.toMinutes();
    if (minutes < 60) {
      return "in " + Math.max(1, minutes) + " Min.";
    }
    long hours = minutes / 60;
    long remainingMinutes = minutes % 60;
    if (hours < 24) {
      return "in "
          + hours
          + " Std."
          + (remainingMinutes == 0 ? "" : " " + remainingMinutes + " Min.");
    }
    long days = hours / 24;
    return "in " + days + " Tag" + (days == 1 ? "" : "en");
  }

  public static String outcome(String value) {
    return "YES".equals(value) ? "JA" : "NO".equals(value) ? "NEIN" : value;
  }

  public static String eventStatus(String value) {
    return switch (value == null ? "" : value) {
      case "OPEN" -> "Offen";
      case "RESOLVED_YES" -> "Mit JA aufgelöst";
      case "RESOLVED_NO" -> "Mit NEIN aufgelöst";
      case "EXPIRED" -> "Erstattet";
      case "ARCHIVED" -> "Archiviert";
      default -> value;
    };
  }

  public static String betStatus(String value) {
    return switch (value == null ? "" : value) {
      case "OPEN" -> "Offen";
      case "WON" -> "Gewonnen";
      case "LOST" -> "Verloren";
      case "REFUNDED" -> "Erstattet";
      default -> value;
    };
  }

  public static List<Bet> groupBets(List<Bet> bets) {
    List<Bet> grouped = new ArrayList<>(bets);
    grouped.sort(
        Comparator.<Bet>comparingInt(bet -> betStatusOrder(bet.status()))
            .thenComparing(Bet::placedAt, Comparator.nullsLast(Comparator.reverseOrder())));
    return grouped;
  }

  public static String live(LiveUpdate update) {
    if (update.type() == LiveUpdate.Type.CONNECTION_ERROR) {
      return "Live-Aktualisierungen sind momentan nicht verfügbar.";
    }
    if (update.type() == LiveUpdate.Type.FEED) {
      return switch (update.action() == null ? "" : update.action()) {
        case "IMPORTED" -> "Ein neuer Markt ist verfügbar.";
        case "RESOLVED_YES" -> "Ein Markt wurde mit JA aufgelöst.";
        case "RESOLVED_NO" -> "Ein Markt wurde mit NEIN aufgelöst.";
        case "EXPIRED" -> "Ein Markt wurde geschlossen und Einsätze wurden erstattet.";
        case "ARCHIVED" -> "Ein abgeschlossener Markt wurde archiviert.";
        default -> "Der Feed wurde aktualisiert.";
      };
    }
    return switch (update.kind() == null ? "" : update.kind()) {
      case "PAYOUT" -> "Gewonnen: " + money(update.amount()) + " wurden gutgeschrieben.";
      case "LOST" -> "Eine deiner Wetten wurde leider verloren.";
      case "REFUND" -> "Erstattung: " + money(update.amount()) + " wurden gutgeschrieben.";
      default -> "Deine Wetten wurden aktualisiert.";
    };
  }

  public static void section(PrintStream output, String title) {
    output.println("\n" + title);
  }

  public static void market(
      PrintStream output, Market market, int position, int total, Instant now) {
    section(output, "Markt " + position + "/" + total + " · " + eventStatus(market.status()));
    output.println(market.question());
    if (market.closesAt() != null) {
      output.println(
          "Wetten bis: " + date(market.closesAt()) + " (" + deadline(market.closesAt(), now) + ")");
    }
    if (market.settlementAt() != null) {
      output.println(
          "Abrechnung: "
              + date(market.settlementAt())
              + " ("
              + deadline(market.settlementAt(), now)
              + ")");
    }
    output.println("Pool: JA " + money(market.yesPool()) + " · NEIN " + money(market.noPool()));
  }

  private static int betStatusOrder(String status) {
    return switch (status == null ? "" : status) {
      case "OPEN" -> 0;
      case "WON" -> 1;
      case "LOST" -> 2;
      case "REFUNDED" -> 3;
      default -> 4;
    };
  }
}
