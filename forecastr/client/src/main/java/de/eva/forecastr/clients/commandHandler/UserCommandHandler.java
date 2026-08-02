package de.eva.forecastr.clients.commandHandler;

import de.eva.forecastr.clients.ClientSession;
import de.eva.forecastr.clients.ConsoleInput;
import de.eva.forecastr.clients.formatter.ConsoleFormatter;
import de.eva.forecastr.core.interfaces.ForecastrGateway;
import de.eva.forecastr.core.models.User;
import de.eva.forecastr.core.models.UserPage;
import de.eva.forecastr.core.models.exceptions.ClientException;
import java.io.PrintStream;
import java.util.List;

public final class UserCommandHandler {
  private final ForecastrGateway gateway;
  private final ClientSession session;
  private final ConsoleInput input;
  private final PrintStream output;

  public UserCommandHandler(
      ForecastrGateway gateway, ClientSession session, ConsoleInput input, PrintStream output) {
    this.gateway = gateway;
    this.session = session;
    this.input = input;
    this.output = output;
  }

  public User chooseUser() {
    int page = 0;
    while (true) {
      UserPage userPage;
      try {
        userPage = gateway.userPage(page, 9);
      } catch (ClientException exception) {
        output.println("\n" + exception.getMessage());
        String action = input.askChoice("[R] Erneut versuchen  [Q] Beenden > ");
        if (action.equalsIgnoreCase("q")) {
          return null;
        }
        continue;
      }
      if (userPage.content().isEmpty() && page > 0) {
        page = Math.max(0, userPage.totalPages() - 1);
        continue;
      }
      ConsoleFormatter.section(output, "Konto auswählen");
      List<User> users = userPage.content();
      for (int index = 0; index < users.size(); index++) {
        output.printf("  [%d] %s%n", index + 1, users.get(index).username());
      }
      output.printf(
          "  Seite %d/%d · %d Konten%n",
          userPage.page() + 1, Math.max(1, userPage.totalPages()), userPage.totalElements());
      if (userPage.page() > 0) {
        output.println("  [Z] Vorherige Seite");
      }
      if (userPage.page() + 1 < userPage.totalPages()) {
        output.println("  [W] Nächste Seite");
      }
      output.println("  [N] Neues Konto erstellen");
      output.println("  [Q] Beenden");
      String choice = input.askChoice("\nAuswahl > ");
      if (choice.equalsIgnoreCase("q")) {
        return null;
      }
      if (choice.equalsIgnoreCase("z") && userPage.page() > 0) {
        page--;
        continue;
      }
      if (choice.equalsIgnoreCase("w") && userPage.page() + 1 < userPage.totalPages()) {
        page++;
        continue;
      }
      if (choice.equalsIgnoreCase("n")) {
        User created = createUser();
        if (created != null) {
          return created;
        }
        continue;
      }
      User selected = select(users, choice);
      if (selected != null) {
        return selected;
      }
      output.println("Bitte wähle eine der angezeigten Optionen.");
    }
  }

  public boolean profile() {
    while (true) {
      ConsoleFormatter.section(output, "Profil");
      output.println("  Benutzername  " + session.user().username());
      String action = input.askChoice("\n[U] Umbenennen   [D] Konto löschen   [X] Hauptmenü > ");
      if (action.equalsIgnoreCase("x")) {
        return true;
      }
      if (action.equalsIgnoreCase("u")) {
        renameUser();
      } else if (action.equalsIgnoreCase("d")) {
        gateway.deleteUser(session.user().id());
        output.println("Das Konto wurde gelöscht.");
        session.logout();
        return false;
      } else {
        output.println("Unbekannte Auswahl.");
      }
    }
  }

  private User createUser() {
    String username = input.ask("Benutzername > ").trim();
    if (username.isEmpty()) {
      output.println("Der Benutzername darf nicht leer sein.");
      return null;
    }
    try {
      return gateway.createUser(username);
    } catch (ClientException exception) {
      output.println("Fehler: " + exception.getMessage());
      return null;
    }
  }

  private void renameUser() {
    String username = input.ask("Neuer Benutzername > ").trim();
    if (username.isEmpty()) {
      output.println("Der Benutzername darf nicht leer sein.");
      return;
    }
    session.user(gateway.updateUser(session.user().id(), username));
    output.println("Der Benutzername wurde geändert.");
  }

  private User select(List<User> users, String choice) {
    try {
      int index = Integer.parseInt(choice) - 1;
      return index >= 0 && index < users.size() ? users.get(index) : null;
    } catch (NumberFormatException exception) {
      return null;
    }
  }
}
