package de.eva.forecastr.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final AdminAuthorizationInterceptor adminAuthorization;

  public WebConfig(AdminAuthorizationInterceptor adminAuthorization) {
    this.adminAuthorization = adminAuthorization;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(adminAuthorization).addPathPatterns("/admin/**", "/stats", "/logs");
  }
}
