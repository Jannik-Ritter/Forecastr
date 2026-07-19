package de.eva.forecastr.rest.restComponents;

import de.eva.forecastr.core.models.exceptions.FailureKind;
import de.eva.forecastr.core.models.exceptions.ForecastrException;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ForecastrException.class)
  ResponseEntity<ErrorBody> forecastr(ForecastrException exception) {
    return body(status(exception.kind()), exception.getMessage());
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class
  })
  ResponseEntity<ErrorBody> badRequest(Exception exception) {
    return body(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ErrorBody> malformedRequest(HttpMessageNotReadableException exception) {
    return body(HttpStatus.BAD_REQUEST, "Malformed JSON request");
  }

  @ExceptionHandler({NoResourceFoundException.class, NoSuchElementException.class})
  ResponseEntity<ErrorBody> missingResource(Exception exception) {
    return body(HttpStatus.NOT_FOUND, "Resource not found");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ErrorBody> unsupportedMethod(HttpRequestMethodNotSupportedException exception) {
    return body(HttpStatus.METHOD_NOT_ALLOWED, exception.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ErrorBody> conflict(DataIntegrityViolationException exception) {
    return body(HttpStatus.CONFLICT, "Request conflicts with existing data");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorBody> validation(MethodArgumentNotValidException exception) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(this::validationMessage)
            .collect(Collectors.joining(", "));
    return body(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler({PessimisticLockingFailureException.class, TaskRejectedException.class})
  ResponseEntity<ErrorBody> unavailable(Exception exception) {
    return body(HttpStatus.SERVICE_UNAVAILABLE, "Server is busy; retry the request");
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorBody> unexpected(Exception exception) {
    return body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
  }

  private HttpStatus status(FailureKind kind) {
    return switch (kind) {
      case NOT_FOUND -> HttpStatus.NOT_FOUND;
      case CONFLICT -> HttpStatus.CONFLICT;
      case FORBIDDEN -> HttpStatus.FORBIDDEN;
      case PAYMENT_REQUIRED -> HttpStatus.PAYMENT_REQUIRED;
    };
  }

  private String validationMessage(FieldError error) {
    return error.getField() + " " + error.getDefaultMessage();
  }

  private ResponseEntity<ErrorBody> body(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(new ErrorBody(Instant.now(), status.value(), message));
  }

  private record ErrorBody(Instant timestamp, int status, String message) {}
}
