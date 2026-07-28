package de.eva.forecastr.core.services;

import de.eva.forecastr.core.models.LogType;
import de.eva.forecastr.core.models.Money;
import de.eva.forecastr.core.models.User;
import de.eva.forecastr.core.models.Wallet;
import de.eva.forecastr.core.models.exceptions.ForecastrException;
import de.eva.forecastr.repository.UserRepository;
import de.eva.forecastr.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {
  private final WalletRepository walletRepository;
  private final UserRepository userRepository;
  private final LogService logService;

  public WalletService(
      WalletRepository walletRepository, UserRepository userRepository, LogService logService) {
    this.walletRepository = walletRepository;
    this.userRepository = userRepository;
    this.logService = logService;
  }

  @Transactional(readOnly = true)
  public Wallet getBalance(Long userId) {
    requireActiveUser(userRepository.findById(userId).orElse(null));
    return walletRepository
        .findById(userId)
        .orElseThrow(() -> ForecastrException.notFound("Wallet not found"));
  }

  @Transactional
  public Wallet deposit(Long userId, BigDecimal amount) {
    requireActiveUser(userRepository.findLocked(userId).orElse(null));
    BigDecimal normalizedAmount = positive(amount);
    Wallet wallet = getLockedWallet(userId);
    wallet.credit(normalizedAmount);
    logService.log(
        LogType.ACCOUNT, Map.of("userId", userId, "action", "DEPOSIT", "amount", normalizedAmount));
    return walletRepository.save(wallet);
  }

  @Transactional
  public Wallet withdraw(Long userId, BigDecimal amount) {
    requireActiveUser(userRepository.findLocked(userId).orElse(null));
    BigDecimal normalizedAmount = positive(amount);
    Wallet wallet = getLockedWallet(userId);
    try {
      wallet.debit(normalizedAmount);
    } catch (IllegalStateException exception) {
      throw ForecastrException.paymentRequired("Insufficient balance");
    }
    logService.log(
        LogType.ACCOUNT,
        Map.of("userId", userId, "action", "WITHDRAW", "amount", normalizedAmount));
    return walletRepository.save(wallet);
  }

  private User requireActiveUser(User user) {
    if (user == null || user.isDeleted()) {
      throw ForecastrException.notFound("User not found");
    }
    return user;
  }

  private Wallet getLockedWallet(Long userId) {
    return walletRepository
        .findLocked(userId)
        .orElseThrow(() -> ForecastrException.notFound("Wallet not found"));
  }

  private BigDecimal positive(BigDecimal amount) {
    BigDecimal normalizedAmount = Money.amount(amount);
    if (normalizedAmount.signum() <= 0) {
      throw new IllegalArgumentException("Amount must be positive");
    }
    return normalizedAmount;
  }
}
