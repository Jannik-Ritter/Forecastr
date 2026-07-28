package de.eva.forecastr.rest.restComponents;

import java.util.List;

public record UserPageResponse(
    List<UserResponse> content, int page, int totalPages, long totalElements) {
  public UserPageResponse {
    content = List.copyOf(content);
  }
}
