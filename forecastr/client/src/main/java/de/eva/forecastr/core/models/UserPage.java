package de.eva.forecastr.core.models;

import java.util.List;

public record UserPage(List<User> content, int page, int totalPages, long totalElements) {
  public UserPage {
    content = List.copyOf(content);
  }
}
