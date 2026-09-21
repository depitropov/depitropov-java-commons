package dev.depitropov.commons.errors;

import static org.apache.commons.lang3.Validate.isTrue;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enumeration of general error codes based on HTTP status codes. Each error code is calculated as:
 * {@code HTTP_STATUS * HTTP_STATUS_ERROR_CODE_BASE + GENERAL_ERROR_CODE_BASE + 99}
 *
 * <p>Error codes provide a standardized way to represent HTTP errors with additional granularity.
 * For example, HTTP 400 (Bad Request) becomes error code 4009999.
 *
 * <p>This enum includes error codes for all standard HTTP 4xx and 5xx status codes, ensuring
 * consistent error handling across the application.
 */
public enum GeneralErrorCode implements ErrorCode {
  BAD_REQUEST_GENERAL(4009999),
  UNAUTHORIZED_GENERAL(4019999),
  PAYMENT_REQUIRED_GENERAL(4029999),
  FORBIDDEN_GENERAL(4039999),
  NOT_FOUND_GENERAL(4049999),
  METHOD_NOT_ALLOWED_GENERAL(4059999),
  NOT_ACCEPTABLE_GENERAL(4069999),
  PROXY_AUTHENTICATION_REQUIRED_GENERAL(4079999),
  REQUEST_TIMEOUT_GENERAL(4089999),
  CONFLICT_GENERAL(4099999),
  GONE_GENERAL(4109999),
  LENGTH_REQUIRED_GENERAL(4119999),
  PRECONDITION_FAILED_GENERAL(4129999),
  PAYLOAD_TOO_LARGE_GENERAL(4139999),
  URI_TOO_LONG_GENERAL(4149999),
  UNSUPPORTED_MEDIA_TYPE_GENERAL(4159999),
  REQUESTED_RANGE_NOT_SATISFIABLE_GENERAL(4169999),
  EXPECTATION_FAILED_GENERAL(4179999),
  I_AM_A_TEAPOT_GENERAL(4189999),
  INSUFFICIENT_SPACE_ON_RESOURCE_GENERAL(4199999),
  METHOD_FAILURE_GENERAL(4209999),
  DESTINATION_LOCKED_GENERAL(4219999),
  UNPROCESSABLE_ENTITY_GENERAL(4229999),
  LOCKED_GENERAL(4239999),
  FAILED_DEPENDENCY_GENERAL(4249999),
  TOO_EARLY_GENERAL(4259999),
  UPGRADE_REQUIRED_GENERAL(4269999),
  PRECONDITION_REQUIRED_GENERAL(4289999),
  TOO_MANY_REQUESTS_GENERAL(4299999),
  REQUEST_HEADER_FIELDS_TOO_LARGE_GENERAL(4319999),
  UNAVAILABLE_FOR_LEGAL_REASONS_GENERAL(4519999),
  INTERNAL_SERVER_ERROR_GENERAL(5009999),
  NOT_IMPLEMENTED_GENERAL(5019999),
  BAD_GATEWAY_GENERAL(5029999),
  SERVICE_UNAVAILABLE_GENERAL(5039999),
  GATEWAY_TIMEOUT_GENERAL(5049999),
  HTTP_VERSION_NOT_SUPPORTED_GENERAL(5059999),
  VARIANT_ALSO_NEGOTIATES_GENERAL(5069999),
  INSUFFICIENT_STORAGE_GENERAL(5079999),
  LOOP_DETECTED_GENERAL(5089999),
  BANDWIDTH_LIMIT_EXCEEDED_GENERAL(5099999),
  NOT_EXTENDED_GENERAL(5109999),
  NETWORK_AUTHENTICATION_REQUIRED_GENERAL(5119999);

  /** Base multiplier for converting HTTP status codes to error codes. */
  public static final int HTTP_STATUS_ERROR_CODE_BASE = 10000;

  /** Base value added to general error codes for identification. */
  public static final int GENERAL_ERROR_CODE_BASE = 9900;

  /** Minimum valid HTTP status code value. */
  public static final int HTTP_STATUS_MIN_VALUE = 100;

  /** Maximum valid HTTP status code value. */
  public static final int HTTP_STATUS_MAX_VALUE = 599;

  /** HTTP status code 500 - Internal Server Error. */
  public static final int INTERNAL_SERVER_ERROR_HTTP_STATUS = 500;

  private final int value;

  private static final Logger log = LoggerFactory.getLogger(GeneralErrorCode.class);

  private static final Map<Integer, ErrorCode> _map = new HashMap<>();

  static {
    for (final GeneralErrorCode iter : values()) {
      _map.put(iter.value, iter);

      httpStatusFromValue(iter.value);
    }
  }

  GeneralErrorCode(final int value) {
    validateErrorCode(value, GENERAL_ERROR_CODE_BASE);

    this.value = value;
  }

  @Override
  public int errorCode() {
    return value;
  }

  @Override
  public String errorMessage() {
    return toString();
  }

  /**
   * Retrieves an ErrorCode for the given error code value with warning logging enabled. If the
   * value is not found, returns INTERNAL_SERVER_ERROR_GENERAL and logs a warning.
   *
   * @param value the error code value to look up
   * @return the corresponding ErrorCode, or INTERNAL_SERVER_ERROR_GENERAL if not found
   */
  public static ErrorCode from(final int value) {
    return from(value, true);
  }

  /**
   * Retrieves an ErrorCode for the given error code value with optional warning logging. Supports
   * both direct error code lookup and HTTP status code conversion.
   *
   * @param value the error code value or HTTP status code to look up
   * @param logWarning if true, logs a warning when error code is not found
   * @return the corresponding ErrorCode, or INTERNAL_SERVER_ERROR_GENERAL if not found
   */
  public static ErrorCode from(final int value, final boolean logWarning) {
    if (_map.containsKey(value)) {
      return _map.get(value);
    }

    if (value >= HTTP_STATUS_MIN_VALUE && value <= HTTP_STATUS_MAX_VALUE) {
      return errorCodeFromHttpStatus(value);
    }

    if (logWarning && log.isWarnEnabled()) {
      final String errMsg = "Failed to find error code for: " + value;
      log.warn(errMsg, new IllegalArgumentException(errMsg));
    }

    return INTERNAL_SERVER_ERROR_GENERAL;
  }

  /**
   * Validates that an error code has the expected base value and valid HTTP status. This is used
   * during enum construction to ensure error code consistency.
   *
   * @param errorCode the error code to validate
   * @param errorCodeBase the expected error code base value
   * @throws IllegalArgumentException if the error code is invalid or has incorrect base
   */
  public static void validateErrorCode(final int errorCode, final int errorCodeBase) {
    httpStatusFromValue(errorCode, true);

    final int base = HTTP_STATUS_ERROR_CODE_BASE / 100;

    final int calculatedErrorCodeBase = ((errorCode / base) % 100) * base;

    isTrue(
        calculatedErrorCodeBase == errorCodeBase,
        "Expected errorCodeBase: %d but found: %d",
        errorCodeBase,
        calculatedErrorCodeBase);
  }

  /**
   * Extracts the HTTP status code from an error code value. If the value is already a valid HTTP
   * status code, returns it directly.
   *
   * @param value the error code value to extract HTTP status from
   * @return the HTTP status code, or 500 if extraction fails
   */
  public static int httpStatusFromValue(final int value) {
    return httpStatusFromValue(value, false);
  }

  /**
   * Extracts the HTTP status code from an error code value with optional failure handling. Supports
   * both direct HTTP status codes and encoded error codes.
   *
   * @param value the error code value to extract HTTP status from
   * @param fail if true, throws exception on invalid HTTP status extraction
   * @return the HTTP status code, or 500 if extraction fails
   * @throws IllegalArgumentException if fail is true and HTTP status extraction fails
   */
  public static int httpStatusFromValue(final int value, final boolean fail) {

    if (value >= HTTP_STATUS_MIN_VALUE && value <= HTTP_STATUS_MAX_VALUE) {
      return value;
    }

    int httpStatus = INTERNAL_SERVER_ERROR_HTTP_STATUS;

    final Integer httpStatusCheck = httpStatusFromValueCheck(value, fail);

    if (httpStatusCheck != null) {
      httpStatus = httpStatusCheck;
    }

    return httpStatus;
  }

  /**
   * Attempts to extract HTTP status code from an error code value, returning null on failure. This
   * is a helper method that provides more granular control over error code validation.
   *
   * @param value the error code value to extract HTTP status from
   * @param fail if true, throws exception on invalid HTTP status; if false, returns null
   * @return the HTTP status code, or null if extraction fails and fail is false
   * @throws IllegalArgumentException if fail is true and HTTP status extraction fails
   */
  public static Integer httpStatusFromValueCheck(final int value, final boolean fail) {

    if (value >= HTTP_STATUS_MIN_VALUE && value <= HTTP_STATUS_MAX_VALUE) {
      return value;
    }

    final int httpStatusFromValue = value / HTTP_STATUS_ERROR_CODE_BASE;

    if (httpStatusFromValue >= HTTP_STATUS_MIN_VALUE
        && httpStatusFromValue <= HTTP_STATUS_MAX_VALUE) {
      return httpStatusFromValue;
    }

    if (fail) {
      throw new IllegalArgumentException(
          String.format("Invalid HttpStatus: %d from errorCode: %d", httpStatusFromValue, value));
    }

    return null;
  }

  /**
   * Creates a general ErrorCode from an HTTP status code. The resulting error code follows the
   * pattern: {@code httpStatus * 10000 + 9999}
   *
   * @param httpStatus the HTTP status code to convert
   * @return the corresponding general ErrorCode
   */
  public static ErrorCode errorCodeFromHttpStatus(final int httpStatus) {
    return GeneralErrorCode.from(httpStatus * HTTP_STATUS_ERROR_CODE_BASE + 9999);
  }
}
