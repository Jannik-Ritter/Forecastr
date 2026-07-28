package de.eva.forecastr.config;

import de.eva.forecastr.core.interfaces.EventSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DefaultDataLoader implements ApplicationRunner {
  private final EventSource eventSource;

  public DefaultDataLoader(EventSource eventSource) {
    this.eventSource = eventSource;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    eventSource.importDefaults();
  }
}
