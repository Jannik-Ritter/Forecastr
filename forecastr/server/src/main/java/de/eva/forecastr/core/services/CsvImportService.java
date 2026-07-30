package de.eva.forecastr.core.services;

import de.eva.forecastr.core.interfaces.EventSource;
import de.eva.forecastr.core.interfaces.ForecastrEventPublisher;
import de.eva.forecastr.core.models.ImportReport;
import de.eva.forecastr.core.models.LogType;
import de.eva.forecastr.core.models.MarketEvent;
import de.eva.forecastr.core.models.Money;
import de.eva.forecastr.core.models.User;
import de.eva.forecastr.core.models.Wallet;
import de.eva.forecastr.repository.EventRepository;
import de.eva.forecastr.repository.UserRepository;
import de.eva.forecastr.repository.WalletRepository;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CsvImportService implements EventSource {
  private final EventRepository eventRepository;
  private final UserRepository userRepository;
  private final WalletRepository walletRepository;
  private final LogService logService;
  private final ForecastrEventPublisher eventPublisher;
  private final Clock clock;
  private final EventCsvRowParser parser;

  public CsvImportService(
      EventRepository eventRepository,
      UserRepository userRepository,
      WalletRepository walletRepository,
      LogService logService,
      ForecastrEventPublisher eventPublisher,
      Clock clock) {
    this.eventRepository = eventRepository;
    this.userRepository = userRepository;
    this.walletRepository = walletRepository;
    this.logService = logService;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
    this.parser = new EventCsvRowParser();
  }

  @Override
  @Transactional
  public ImportReport importDefaults() {
    try (Reader userReader = resource("data/users.csv");
        Reader eventReader = resource("data/events.csv")) {
      importUsers(userReader);
      return importEvents(eventReader);
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot read bundled CSV data", exception);
    }
  }

  @Override
  @Transactional
  public ImportReport importPath(Path path) {
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      return importEvents(reader);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Cannot read CSV path: " + exception.getMessage());
    }
  }

  @Override
  @Transactional
  public ImportReport importEvents(Reader reader) {
    int accepted = 0;
    int rejected = 0;
    int skipped = 0;
    List<String> errors = new ArrayList<>();
    Instant importedAt = clock.instant();
    try (CSVParser csv = csv(reader)) {
      for (CSVRecord record : csv) {
        try {
          MarketEvent event = parser.parse(record.toMap(), importedAt);
          if (eventRepository.existsById(event.getId())) {
            skipped++;
            continue;
          }
          eventRepository.save(event);
          accepted++;
          logService.log(LogType.EVENT_IMPORT, Map.of("eventId", event.getId(), "accepted", true));
          eventPublisher.eventChanged(event.getId(), "IMPORTED");
        } catch (RuntimeException exception) {
          rejected++;
          String message = "row " + record.getRecordNumber() + ": " + exception.getMessage();
          errors.add(message);
          logService.log(
              LogType.EVENT_IMPORT,
              Map.of(
                  "row", record.getRecordNumber(),
                  "accepted", false,
                  "reason", exception.getMessage()));
        }
      }
    } catch (IOException | IllegalArgumentException exception) {
      throw new IllegalArgumentException(
          "Invalid events CSV: " + exception.getMessage(), exception);
    }
    eventPublisher.importsRejected(rejected);
    return new ImportReport(accepted, rejected, skipped, List.copyOf(errors));
  }

  @Transactional
  public void importUsers(Reader reader) {
    try (CSVParser csv = csv(reader)) {
      for (CSVRecord record : csv) {
        importUser(record);
      }
    } catch (IOException exception) {
      throw new IllegalArgumentException("Invalid users CSV", exception);
    }
  }

  private void importUser(CSVRecord record) {
    String username = record.get("username").trim();
    BigDecimal balance = Money.amount(new BigDecimal(record.get("initialBalance")));
    boolean isAdmin = parseBoolean(record.get("isAdmin"), record.getRecordNumber());
    if (username.isBlank() || balance.signum() < 0) {
      throw new IllegalArgumentException("Invalid user row " + record.getRecordNumber());
    }
    if (userRepository.existsByUsernameIgnoreCase(username)) {
      return;
    }
    User user = userRepository.save(new User(username, clock.instant(), isAdmin));
    walletRepository.save(new Wallet(user.getId(), balance));
    logService.log(
        LogType.ACCOUNT, Map.of("userId", user.getId(), "action", "IMPORTED", "balance", balance));
  }

  private CSVParser csv(Reader reader) throws IOException {
    return CSVFormat.DEFAULT
        .builder()
        .setHeader()
        .setSkipHeaderRecord(true)
        .setTrim(true)
        .build()
        .parse(reader);
  }

  private boolean parseBoolean(String value, long recordNumber) {
    if ("true".equalsIgnoreCase(value)) {
      return true;
    }
    if ("false".equalsIgnoreCase(value)) {
      return false;
    }
    throw new IllegalArgumentException(
        "Invalid isAdmin value in user row " + recordNumber + ": " + value);
  }

  private Reader resource(String name) throws IOException {
    return new InputStreamReader(
        new ClassPathResource(name).getInputStream(), StandardCharsets.UTF_8);
  }
}
