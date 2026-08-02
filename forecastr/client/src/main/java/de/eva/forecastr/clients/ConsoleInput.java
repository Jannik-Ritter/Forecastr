package de.eva.forecastr.clients;

import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Scanner;

public final class ConsoleInput {
  private final Scanner scanner;
  private final PrintStream output;

  public ConsoleInput(InputStream input, PrintStream output) {
    this.scanner = new Scanner(input);
    this.output = output;
  }

  public String ask(String prompt) {
    output.print(prompt);
    output.flush();
    if (!scanner.hasNextLine()) {
      throw new EndOfInputException();
    }
    return scanner.nextLine().trim();
  }

  public String askChoice(String prompt) {
    String choice;
    do {
      choice = ask(prompt);
    } while (choice.isEmpty());
    return choice;
  }

  public BigDecimal amount(String value) {
    if (value.equalsIgnoreCase("x")) {
      return null;
    }
    try {
      BigDecimal amount = new BigDecimal(value.replace(',', '.')).setScale(2);
      if (amount.signum() <= 0) {
        throw new NumberFormatException();
      }
      return amount;
    } catch (ArithmeticException | NumberFormatException exception) {
      output.println("Bitte gib einen positiven Betrag mit höchstens zwei Nachkommastellen ein.");
      return null;
    }
  }

  public Integer nonNegativeInt(String value) {
    try {
      int number = Integer.parseInt(value);
      if (number < 0) {
        throw new NumberFormatException();
      }
      return number;
    } catch (NumberFormatException exception) {
      output.println("Bitte gib eine nicht-negative ganze Zahl ein.");
      return null;
    }
  }

  public Integer positiveInt(String value) {
    return boundedPositiveInt(value, Integer.MAX_VALUE);
  }

  public Integer boundedPositiveInt(String value, int maximum) {
    try {
      int number = Integer.parseInt(value);
      if (number < 1 || number > maximum) {
        throw new NumberFormatException();
      }
      return number;
    } catch (NumberFormatException exception) {
      output.println("Bitte gib eine ganze Zahl zwischen 1 und " + maximum + " ein.");
      return null;
    }
  }

  public Long positiveLong(String value) {
    try {
      long number = Long.parseLong(value);
      if (number < 1) {
        throw new NumberFormatException();
      }
      return number;
    } catch (NumberFormatException exception) {
      output.println("Bitte gib eine positive Ereignis-ID ein.");
      return null;
    }
  }

  public String outcome(String value) {
    return switch (value.toLowerCase(Locale.ROOT)) {
      case "j", "ja", "yes" -> "YES";
      case "n", "nein", "no" -> "NO";
      default -> {
        output.println("Bitte gib JA oder NEIN ein.");
        yield null;
      }
    };
  }

  public static final class EndOfInputException extends RuntimeException {}
}
