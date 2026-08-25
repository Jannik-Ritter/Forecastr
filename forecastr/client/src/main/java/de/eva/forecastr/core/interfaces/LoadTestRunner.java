package de.eva.forecastr.core.interfaces;

import de.eva.forecastr.core.models.LoadTestProgress;
import java.util.function.Consumer;

public interface LoadTestRunner {
  boolean runQuick();

  boolean runFull();

  default boolean runQuick(Consumer<LoadTestProgress> progress) {
    return runQuick();
  }

  default boolean runFull(Consumer<LoadTestProgress> progress) {
    return runFull();
  }

  boolean execute(String command);

  static LoadTestRunner unavailable() {
    return new LoadTestRunner() {
      @Override
      public boolean runQuick() {
        return false;
      }

      @Override
      public boolean runFull() {
        return false;
      }

      @Override
      public boolean execute(String command) {
        return false;
      }
    };
  }
}
