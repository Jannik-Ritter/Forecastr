package de.eva.forecastr.rest.restComponents;

import de.eva.forecastr.core.models.User;
import de.eva.forecastr.core.services.UserService;
import de.eva.forecastr.core.services.WalletService;
import de.eva.forecastr.rest.createRecords.CreateUserRequest;
import de.eva.forecastr.rest.createRecords.MoneyRequest;
import de.eva.forecastr.rest.createRecords.UpdateUserRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {
  private final UserService userService;
  private final WalletService walletService;

  public UserController(UserService userService, WalletService walletService) {
    this.userService = userService;
    this.walletService = walletService;
  }

  @GetMapping
  List<UserResponse> getUsers() {
    return userService.getActiveUsers().stream().map(RestMapper::user).toList();
  }

  @GetMapping("/page")
  UserPageResponse getUserPage(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "9") int size) {
    Page<User> users = userService.getActiveUsers(page, size);
    return new UserPageResponse(
        users.getContent().stream().map(RestMapper::user).toList(),
        users.getNumber(),
        users.getTotalPages(),
        users.getTotalElements());
  }

  @PostMapping
  ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    User user = userService.createUser(request.username(), request.initialBalance());
    return ResponseEntity.status(HttpStatus.CREATED).body(RestMapper.user(user));
  }

  @GetMapping("/{id}")
  UserResponse getUser(@PathVariable Long id) {
    return RestMapper.user(userService.getUserById(id));
  }

  @PutMapping("/{id}")
  UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
    return RestMapper.user(userService.updateUser(id, request.username()));
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/deposit")
  BalanceResponse deposit(@PathVariable Long id, @Valid @RequestBody MoneyRequest request) {
    return RestMapper.wallet(walletService.deposit(id, request.amount()));
  }

  @PostMapping("/{id}/withdraw")
  BalanceResponse withdraw(@PathVariable Long id, @Valid @RequestBody MoneyRequest request) {
    return RestMapper.wallet(walletService.withdraw(id, request.amount()));
  }

  @GetMapping("/{id}/balance")
  BalanceResponse getBalance(@PathVariable Long id) {
    return RestMapper.wallet(walletService.getBalance(id));
  }

}
