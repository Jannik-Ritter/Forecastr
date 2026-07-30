package de.eva.forecastr.core.interfaces;

import de.eva.forecastr.core.models.ImportReport;
import java.io.Reader;
import java.nio.file.Path;

public interface EventSource {
  ImportReport importEvents(Reader reader);

  ImportReport importDefaults();

  ImportReport importPath(Path path);
}
