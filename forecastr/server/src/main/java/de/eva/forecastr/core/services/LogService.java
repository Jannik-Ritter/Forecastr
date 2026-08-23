package de.eva.forecastr.core.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.eva.forecastr.core.models.LogEntry;
import de.eva.forecastr.core.models.LogType;
import de.eva.forecastr.repository.LogEntryRepository;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class LogService {
  private static final Logger LOGGER = LoggerFactory.getLogger(LogService.class);

  private final LogEntryRepository logEntryRepository;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public LogService(LogEntryRepository logEntryRepository, ObjectMapper objectMapper, Clock clock) {
    this.logEntryRepository = logEntryRepository;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  public LogEntry log(LogType type, Object payload) {
    String json;
    try {
      json = objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      json = "{\"error\":\"payload serialization failed\"}";
    }
    String threadName = Thread.currentThread().getName();
    LOGGER.info("{} operation on thread {}: {}", type, threadName, json);
    return logEntryRepository.save(new LogEntry(clock.instant(), type, json, threadName));
  }

  public List<LogEntry> getLogs(LogType type, int limit) {
    Pageable page = PageRequest.of(0, Math.max(1, Math.min(limit, 10_000)));
    return type == null
        ? logEntryRepository.findAllByOrderByTimestampDesc(page)
        : logEntryRepository.findByTypeOrderByTimestampDesc(type, page);
  }
}
