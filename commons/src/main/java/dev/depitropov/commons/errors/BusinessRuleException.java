package dev.depitropov.commons.errors;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.time.Instant;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Exception representing a business rule violation with an associated error code. This exception is
 * used throughout the application to signal business logic errors with structured error information
 * including error codes, messages, and timestamps.
 *
 * <p>The exception message is formatted as "[ErrorCode] message" for consistent error reporting.
 */
@Slf4j
@Getter
public class BusinessRuleException extends RuntimeException {

  private final Instant timestamp;

  private final ErrorCode errorCode;

  private final String originalMessage;

  /**
   * Creates a BusinessRuleException with an error code and message.
   *
   * @param errorCode the error code representing the type of business rule violation
   * @param message the error message (supports String.format placeholders)
   * @param arguments the arguments for the message format
   */
  public BusinessRuleException(
      final ErrorCode errorCode, final String message, final Object... arguments) {
    this(null, null, errorCode, message, arguments);
  }

  /**
   * Creates a BusinessRuleException wrapping another exception with an error code and message.
   *
   * @param exception the underlying exception that caused this business rule violation
   * @param errorCode the error code representing the type of business rule violation
   * @param message the error message (supports String.format placeholders)
   * @param arguments the arguments for the message format
   */
  public BusinessRuleException(
      final Throwable exception,
      final ErrorCode errorCode,
      final String message,
      final Object... arguments) {
    this(exception, null, errorCode, message, arguments);
  }

  /**
   * Creates a BusinessRuleException with full control over all fields. This is the primary
   * constructor that all other constructors delegate to.
   *
   * @param exception the underlying exception that caused this business rule violation (may be
   *     null)
   * @param timestamp the timestamp when the error occurred (uses current time if null)
   * @param errorCode the error code representing the type of business rule violation (required)
   * @param message the error message (supports String.format placeholders)
   * @param arguments the arguments for the message format
   * @throws NullPointerException if errorCode is null
   */
  public BusinessRuleException(
      final Throwable exception,
      final Instant timestamp,
      final ErrorCode errorCode,
      final String message,
      final Object... arguments) {
    super(buildMessage(errorCode, message, arguments), exception);

    requireNonNull(errorCode);

    this.timestamp = timestamp != null ? timestamp : Instant.now();
    this.errorCode = errorCode;

    originalMessage = safeFormat(message, arguments);
  }

  /**
   * Validates a condition and throws a BusinessRuleException if the condition is false. This is a
   * convenience method for conditional validation.
   *
   * @param condition the condition to validate
   * @param errorCode the error code to use if validation fails
   * @param message the error message to use if validation fails (supports String.format
   *     placeholders)
   * @param arguments the arguments for the message format
   * @throws BusinessRuleException if condition is false
   */
  public static void isValid(
      final boolean condition,
      final ErrorCode errorCode,
      final String message,
      final Object... arguments) {
    if (!condition) {
      throw new BusinessRuleException(errorCode, message, arguments);
    }
  }

  /**
   * Builds a formatted exception message with error code prefix. The resulting format is
   * "[ErrorCode] message" or just "[ErrorCode]" if message is empty.
   *
   * @param errorCode the error code to include in the message
   * @param message the error message (supports String.format placeholders)
   * @param arguments the arguments for the message format
   * @return the formatted message with error code prefix
   */
  public static String buildMessage(
      final ErrorCode errorCode, final String message, final Object... arguments) {

    final String formattedMessage = safeFormat(message, arguments);

    final StringBuilder sb = new StringBuilder();

    if (errorCode != null) {
      sb.append("[").append(errorCode).append("]");

      if (isNotEmpty(formattedMessage)) {
        sb.append(" ");
      }
    }

    if (isNotEmpty(formattedMessage)) {
      sb.append(formattedMessage);
    }

    return sb.toString();
  }

  /**
   * Safely formats a message using String.format, handling formatting errors gracefully. If
   * formatting fails, logs a warning and returns the original unformatted message.
   *
   * @param message the message template (supports String.format placeholders)
   * @param arguments the arguments for the message format
   * @return the formatted message, or the original message if formatting fails, or null if message
   *     is null/blank
   */
  public static String safeFormat(String message, final Object... arguments) {

    message = trimToNull(message);

    try {
      return isNotEmpty(message) ? format(message, arguments) : null;
    } catch (final Exception e) {
      log.warn("Error formatting message: {}, error: {}", message, e.getMessage(), e);

      return message;
    }
  }
}
