package de.eva.forecastr.config;

import de.eva.forecastr.core.models.exceptions.ForecastrException;
import de.eva.forecastr.core.services.AdminAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthorizationInterceptor implements HandlerInterceptor {
  public static final String ACTOR_HEADER = "X-Forecastr-User-Id";

  private final AdminAccessService adminAccessService;

  public AdminAuthorizationInterceptor(AdminAccessService adminAccessService) {
    this.adminAccessService = adminAccessService;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    adminAccessService.requireAdmin(parseActorId(request.getHeader(ACTOR_HEADER)));
    return true;
  }

  private long parseActorId(String value) {
    try {
      return Long.parseLong(value == null ? "" : value.trim());
    } catch (NumberFormatException exception) {
      throw ForecastrException.forbidden("Admin account required");
    }
  }
}
