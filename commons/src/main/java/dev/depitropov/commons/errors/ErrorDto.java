package dev.depitropov.commons.errors;

import static dev.depitropov.commons.DEObjectUtils.safeGet;
import static dev.depitropov.commons.DETimeUtils.DATE_TIME_FORMAT_ISO;
import static dev.depitropov.commons.DETimeUtils.UTC;
import static dev.depitropov.commons.errors.GeneralErrorCode.INTERNAL_SERVER_ERROR_GENERAL;
import static dev.depitropov.commons.errors.GeneralErrorCode.httpStatusFromValue;
import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Data Transfer Object for representing error information in a standardized format. Used for
 * serializing exceptions and errors to JSON for API responses and error propagation.
 *
 * <p>Contains all necessary information about an error including:
 *
 * <ul>
 *   <li>Timestamp when the error occurred
 *   <li>HTTP status code
 *   <li>Application-specific error code
 *   <li>Error message
 *   <li>Additional error details
 *   <li>Original exception cause (not serialized)
 * </ul>
 *
 * <p>This DTO can be easily serialized to JSON and deserialized, making it suitable for REST API
 * error responses and inter-service error communication.
 */
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorDto {

  /** Timestamp when the error occurred, formatted as ISO-8601 string in JSON. */
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_TIME_FORMAT_ISO, timezone = UTC)
  private final Instant timestamp;

  /** HTTP status code associated with this error. */
  private final int httpStatus;

  /** Application-specific error code. */
  private final int errorCode;

  /** Human-readable error message. */
  private final String errorMessage;

  /** Additional error details providing context about the error. */
  private final List<String> errorDetails;

  /** Original exception that caused this error (not included in JSON serialization). */
  @JsonIgnore private final Throwable cause;

  /** Creates an ErrorDto with default INTERNAL_SERVER_ERROR_GENERAL error code. */
  public ErrorDto() {
    this(INTERNAL_SERVER_ERROR_GENERAL);
  }

  /**
   * Creates an ErrorDto with the specified error code and no additional details.
   *
   * @param errorCode the error code to use
   */
  public ErrorDto(final ErrorCode errorCode) {
    this(errorCode, Collections.emptyList());
  }

  /**
   * Creates an ErrorDto with the specified error code and error details.
   *
   * @param errorCode the error code to use
   * @param errorDetails additional error details
   */
  public ErrorDto(final ErrorCode errorCode, final List<String> errorDetails) {
    this(errorCode, errorDetails, null);
  }

  /**
   * Creates an ErrorDto with the specified error code, error details, and cause. The timestamp is
   * set to the current time.
   *
   * @param errorCode the error code to use
   * @param errorDetails additional error details
   * @param cause the original exception that caused this error
   */
  public ErrorDto(
      final ErrorCode errorCode, final List<String> errorDetails, final Throwable cause) {
    this(Instant.now(), errorCode, errorDetails, cause);
  }

  /**
   * Creates an ErrorDto with full control over all fields. This is the primary constructor that all
   * other constructors delegate to.
   *
   * @param timestamp the timestamp when the error occurred
   * @param errorCode the error code to use
   * @param errorDetails additional error details
   * @param cause the original exception that caused this error
   * @throws NullPointerException if timestamp, errorCode, or errorDetails is null
   */
  public ErrorDto(
      final Instant timestamp,
      final ErrorCode errorCode,
      final List<String> errorDetails,
      final Throwable cause) {

    requireNonNull(timestamp);
    requireNonNull(errorCode);
    requireNonNull(errorDetails);

    this.timestamp = timestamp;
    this.errorCode = errorCode.errorCode();
    this.errorDetails = errorDetails;
    this.cause = cause;

    httpStatus = httpStatusFromValue(errorCode.errorCode());

    errorMessage = errorCode.errorMessage();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("timestamp", timestamp)
        .append("timestamp.epoch", safeGet(timestamp, Instant::toEpochMilli))
        .append("httpStatus", httpStatus)
        .append("errorCode", errorCode)
        .append("errorMessage", errorMessage)
        .append("errorDetails", errorDetails)
        .append("cause", cause)
        .toString();
  }
}
