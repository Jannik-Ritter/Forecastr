package de.eva.forecastr.rest.restComponents;

import de.eva.forecastr.core.models.User;
import de.eva.forecastr.core.models.Wallet;

public final class RestMapper {
  private RestMapper() {}

  public static UserResponse user(User user) {
    return new UserResponse(
        user.getId(), user.getUsername(), user.isAdmin(), user.getCreatedAt(), user.getDeletedAt());
  }

  public static BalanceResponse wallet(Wallet wallet) {
    return new BalanceResponse(wallet.getUserId(), wallet.getBalance(), "EUR", wallet.getVersion());
  }

}
