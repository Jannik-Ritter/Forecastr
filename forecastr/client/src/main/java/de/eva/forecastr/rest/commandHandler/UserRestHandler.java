package de.eva.forecastr.rest.commandHandler;

import de.eva.forecastr.core.models.User;
import de.eva.forecastr.core.models.UserPage;
import de.eva.forecastr.rest.CommunicationHandler;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class UserRestHandler {
  private final CommunicationHandler communicationHandler;

  public UserRestHandler(CommunicationHandler communicationHandler) {
    this.communicationHandler = communicationHandler;
  }

  public List<User> getUsers() {
    return communicationHandler.values(communicationHandler.get("/users"), User.class);
  }

  public UserPage getUserPage(int page, int size) {
    String path = "/users/page?page=" + Math.max(0, page) + "&size=" + Math.max(1, size);
    return communicationHandler.value(communicationHandler.get(path), UserPage.class);
  }

  public User createUser(String username) {
    return communicationHandler.value(
        communicationHandler.post(
            "/users", Map.of("username", username, "initialBalance", new BigDecimal("100.00"))),
        User.class);
  }

  public User updateUser(long userId, String username) {
    return communicationHandler.value(
        communicationHandler.put("/users/" + userId, Map.of("username", username)), User.class);
  }

  public void deleteUser(long userId) {
    communicationHandler.requireSuccess(communicationHandler.delete("/users/" + userId));
  }
}
