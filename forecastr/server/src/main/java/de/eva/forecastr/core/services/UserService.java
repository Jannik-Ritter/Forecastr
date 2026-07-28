package de.eva.forecastr.core.services;

import de.eva.forecastr.core.models.LogType;
import de.eva.forecastr.core.models.Money;
import de.eva.forecastr.core.models.User;
import de.eva.forecastr.core.models.Wallet;
import de.eva.forecastr.core.models.exceptions.ForecastrException;
import de.eva.forecastr.repository.UserRepository;
import de.eva.forecastr.repository.WalletRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserRepository userRepository;
  private final WalletRepository walletRepository;
  private final LogService logService;
  private final Clock clock;

  public UserService(
      UserRepository userRepository,
      WalletRepository walletRepository,
      LogService logService,
      Clock clock) {
    this.userRepository = userRepository;
    this.walletRepository = walletRepository;
    this.logService = logService;
    this.clock = clock;
  }

  @Transactional
  public User createUser(String username, BigDecimal initialBalance) {
    String normalizedUsername = normalize(username);
    if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
      throw ForecastrException.conflict("Username already exists");
    }
    BigDecimal balance = initialBalance == null ? Money.ZERO : Money.amount(initialBalance);
    if (balance.signum() < 0) {
      throw new IllegalArgumentException("Initial balance cannot be negative");
    }
    try {
      User user = userRepository.save(new User(normalizedUsername, clock.instant(), false));
      walletRepository.save(new Wallet(user.getId(), balance));
      logService.log(LogType.ACCOUNT, Map.of("userId", user.getId(), "action", "CREATED"));
      return user;
    } catch (DataIntegrityViolationException exception) {
      throw ForecastrException.conflict("Username already exists");
    }
  }

  @Transactional(readOnly = true)
  public User getUserById(Long userId) {
    return requireActiveUser(userRepository.findById(userId).orElse(null));
  }

  @Transactional(readOnly = true)
  public List<User> getActiveUsers() {
    return userRepository.findByDeletedAtIsNullOrderByUsernameAsc();
  }

  @Transactional(readOnly = true)
  public Page<User> getActiveUsers(int page, int size) {
    int safePage = Math.max(0, page);
    int safeSize = Math.max(1, Math.min(size, 100));
    Sort sort = Sort.by(Sort.Order.asc("username").ignoreCase());
    return userRepository.findByDeletedAtIsNull(PageRequest.of(safePage, safeSize, sort));
  }

  @Transactional
  public User updateUser(Long userId, String username) {
    User user = requireActiveUser(userRepository.findLocked(userId).orElse(null));
    String normalizedUsername = normalize(username);
    userRepository
        .findByUsernameIgnoreCase(normalizedUsername)
        .filter(other -> !other.getId().equals(userId))
        .ifPresent(
            other -> {
              throw ForecastrException.conflict("Username already exists");
            });
    user.setUsername(normalizedUsername);
    logService.log(LogType.ACCOUNT, Map.of("userId", userId, "action", "UPDATED"));
    try {
      return userRepository.save(user);
    } catch (DataIntegrityViolationException exception) {
      throw ForecastrException.conflict("Username already exists");
    }
  }

  @Transactional
  public void deleteUser(Long userId) {
    User user = requireActiveUser(userRepository.findLocked(userId).orElse(null));
    Wallet wallet =
        walletRepository
            .findLocked(userId)
            .orElseThrow(() -> ForecastrException.notFound("Wallet not found"));
    walletRepository.delete(wallet);
    user.softDelete(clock.instant());
    userRepository.save(user);
    logService.log(LogType.ACCOUNT, Map.of("userId", userId, "action", "DELETED"));
  }

  private User requireActiveUser(User user) {
    if (user == null || user.isDeleted()) {
      throw ForecastrException.notFound("User not found");
    }
    return user;
  }

  private String normalize(String username) {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Username is required");
    }
    return username.trim();
  }
}
