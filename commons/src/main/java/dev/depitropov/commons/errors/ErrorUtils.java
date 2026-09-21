package dev.depitropov.commons.errors;

import static dev.depitropov.commons.DEStringUtils.hideString;
import static dev.depitropov.commons.DEUuidUtils.DEFAULT_MAX_ID_SIZE;
import static dev.depitropov.commons.DEUuidUtils.isValidId;
import static dev.depitropov.commons.errors.BusinessRuleException.isValid;
import static dev.depitropov.commons.errors.GeneralErrorCode.BAD_REQUEST_GENERAL;
import static dev.depitropov.commons.errors.GeneralErrorCode.CONFLICT_GENERAL;
import static dev.depitropov.commons.errors.GeneralErrorCode.FORBIDDEN_GENERAL;
import static dev.depitropov.commons.errors.GeneralErrorCode.HTTP_STATUS_ERROR_CODE_BASE;
import static dev.depitropov.commons.errors.GeneralErrorCode.HTTP_STATUS_MAX_VALUE;
import static dev.depitropov.commons.errors.GeneralErrorCode.INTERNAL_SERVER_ERROR_GENERAL;
import static dev.depitropov.commons.errors.GeneralErrorCode.NOT_FOUND_GENERAL;
import static dev.depitropov.commons.errors.GeneralErrorCode.httpStatusFromValue;
import static dev.depitropov.commons.errors.GeneralErrorCode.httpStatusFromValueCheck;
import static java.lang.String.format;
import static java.util.Objects.*;
import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.exception.ExceptionUtils.getThrowableList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.depitropov.commons.ClassTypeProvider;
import dev.depitropov.commons.ObjectSerializer;
import dev.depitropov.commons.errors.msg.BusinessRuleExceptionMessageExtractor;
import dev.depitropov.commons.errors.msg.ConstraintViolationExceptionMessageExtractor;
import dev.depitropov.commons.impl.ObjectSerializerImpl;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class ErrorUtils {

  public static final String JSON_RESPONSE_MESSAGE_KEY = "message";

  public static final String JSON_RESPONSE_ERROR_KEY = "error";

  private static final ObjectSerializer defaultObjectSerializer;

  private static final Map<Class<?>, ExceptionMessageExtractor<?>>
      classToExceptionMessageExtractor = new HashMap<>();

  static {
    defaultObjectSerializer =
        new ObjectSerializerImpl(new ObjectMapper().registerModule(new JavaTimeModule()));

    registerExceptionMessageExtractors(
        List.of(
            new BusinessRuleExceptionMessageExtractor(true),
            new ConstraintViolationExceptionMessageExtractor()));
  }

  /**
   * Finds the first throwable in the exception chain that matches the specified class.
   *
   * @param t the throwable to search
   * @param tClass the class of throwable to find
   * @param <T> the type of throwable
   * @return the first matching throwable, or null if not found
   */
  public static <T extends Throwable> T findThrowable(final Throwable t, final Class<T> tClass) {
    return getThrowableList(t).stream()
        .filter(tClass::isInstance)
        .map(tClass::cast)
        .findFirst()
        .orElse(null);
  }

  public record ExceptionErrorInfo(
      List<String> classNames,
      List<String> fullClassNames,
      List<String> messages,
      List<ErrorCode> errorCodes) {}

  /**
   * Extracts simple class names from all exceptions in the throwable chain.
   *
   * @param t the throwable to extract class names from
   * @return list of simple class names
   */
  @NotEmpty
  public static List<String> getExceptionClassNames(final Throwable t) {
    return getExceptionErrorInfo(t, false).classNames;
  }

  /**
   * Extracts fully qualified class names from all exceptions in the throwable chain.
   *
   * @param t the throwable to extract full class names from
   * @return list of fully qualified class names
   */
  @NotEmpty
  public static List<String> getExceptionFullClassNames(final Throwable t) {
    return getExceptionErrorInfo(t, false).fullClassNames;
  }

  /**
   * Extracts messages from all exceptions in the throwable chain with class names included.
   *
   * @param t the throwable to extract messages from
   * @return list of exception messages formatted as "ClassName(message)"
   */
  @NotEmpty
  public static List<String> getExceptionMessages(final Throwable t) {
    return getExceptionMessages(t, true);
  }

  /**
   * Extracts messages from all exceptions in the throwable chain.
   *
   * @param t the throwable to extract messages from
   * @param addClassNameToMessage if true, formats messages as "ClassName(message)", otherwise just
   *     returns the message
   * @return list of exception messages
   */
  @NotEmpty
  public static List<String> getExceptionMessages(
      final Throwable t, final boolean addClassNameToMessage) {
    return getExceptionErrorInfo(t, addClassNameToMessage).messages;
  }

  /**
   * Extracts comprehensive error information from all exceptions in the throwable chain. This
   * includes class names, full class names, messages, and error codes.
   *
   * @param t the throwable to extract information from
   * @param addClassNameToMessage if true, formats messages as "ClassName(message)", otherwise just
   *     returns the message
   * @return ExceptionErrorInfo containing all extracted error information
   */
  @NotNull
  public static ExceptionErrorInfo getExceptionErrorInfo(
      final Throwable t, final boolean addClassNameToMessage) {
    final List<Throwable> throwables = getThrowableList(t);

    final List<String> classNames = new ArrayList<>();
    final List<String> fullClassNames = new ArrayList<>();
    final List<String> messages = new ArrayList<>();
    final List<ErrorCode> errorCodes = new ArrayList<>();

    throwables.forEach(
        iter -> {
          final ExceptionMessageExtractor<?> extractor = resolveExtractorForClass(iter.getClass());

          String message = "";

          if (extractor != null) {
            final ErrorCode errorCode = extractor.getErrorCode(iter);

            message = trimToEmpty(extractor.getMessage(iter));

            errorCodes.add(errorCode);
          }

          if (message.isEmpty()) {
            message =
                trimToEmpty(
                    iter.getMessage() != null ? iter.getMessage() : iter.getLocalizedMessage());
          }

          final Throwable cause = iter.getCause();
          if (cause != null && message.startsWith(cause.getClass().getName())) {
            message = cause.getClass().getSimpleName();
          }

          final String className = iter.getClass().getSimpleName();

          if (!classNames.contains(className)) {
            classNames.add(className);
          }

          if (!fullClassNames.contains(iter.getClass().getName())) {
            fullClassNames.add(iter.getClass().getName());
          }

          final String toAdd;
          if (message.isEmpty()) {
            toAdd = className;
          } else if (addClassNameToMessage) {
            toAdd = format("%s(%s)", className, message);
          } else {
            toAdd = message;
          }

          if (!messages.contains(toAdd)) {
            messages.add(toAdd);
          }
        });

    return new ExceptionErrorInfo(classNames, fullClassNames, messages, errorCodes);
  }

  private static ExceptionMessageExtractor<?> resolveExtractorForClass(
      final Class<? extends Throwable> clazz) {
    ExceptionMessageExtractor<?> res = classToExceptionMessageExtractor.get(clazz);

    if (res == null) {
      final Entry<Class<?>, ExceptionMessageExtractor<?>> extractorClass =
          classToExceptionMessageExtractor.entrySet().stream()
              .filter(iter -> iter.getKey().isAssignableFrom(clazz))
              .findFirst()
              .orElse(null);

      if (extractorClass != null) {
        res = extractorClass.getValue();
      }
    }

    return res;
  }

  /**
   * Registers custom exception message extractors for specific exception types. These extractors
   * are used to extract error codes and messages from custom exceptions.
   *
   * @param exceptionMessageExtractors list of extractors to register
   */
  public static void registerExceptionMessageExtractors(
      final List<ExceptionMessageExtractor<?>> exceptionMessageExtractors) {
    exceptionMessageExtractors.forEach(
        exceptionMessageExtractor ->
            classToExceptionMessageExtractor.put(
                exceptionMessageExtractor.getExceptionClass(), exceptionMessageExtractor));
  }

  /**
   * Returns the exception message or the exception class simple name if the message is empty.
   *
   * @param t the throwable to get the message from
   * @return the exception message, or the class simple name if message is empty
   */
  @NotEmpty
  public static String getNotEmptyMessageOrExceptionClassSimpleName(final Throwable t) {
    final String message =
        trimToEmpty(t.getMessage() != null ? t.getMessage() : t.getLocalizedMessage());

    return message.isEmpty() ? t.getClass().getSimpleName() : message;
  }

  /**
   * Validates a condition and throws BAD_REQUEST BusinessRuleException if false.
   *
   * @param condition the condition to validate
   * @param message the error message (supports String.format placeholders)
   * @param arguments the arguments for the message format
   * @throws BusinessRuleException with BAD_REQUEST_GENERAL error code if condition is false
   */
  public static void isValidArgument(
      final boolean condition, final String message, final Object... arguments) {
    isValid(condition, BAD_REQUEST_GENERAL, message, arguments);
  }

  /**
   * Validates that a resource was found and throws NOT_FOUND BusinessRuleException if false.
   *
   * @param condition the condition to validate (typically a resource exists check)
   * @param message the error message (supports String.format placeholders)
   * @param arguments the arguments for the message format
   * @throws BusinessRuleException with NOT_FOUND_GENERAL error code if condition is false
   */
  public static void isFoundResource(
      final boolean condition, final String message, final Object... arguments) {
    isValid(condition, NOT_FOUND_GENERAL, message, arguments);
  }

  /**
   * Validates that an operation is allowed and throws FORBIDDEN BusinessRuleException if false.
   *
   * @param condition the condition to validate (typically a permission check)
   * @param message the error message (supports String.format placeholders)
   * @param arguments the arguments for the message format
   * @throws BusinessRuleException with FORBIDDEN_GENERAL error code if condition is false
   */
  public static void isAllowed(
      final boolean condition, final String message, final Object... arguments) {
    isValid(condition, FORBIDDEN_GENERAL, message, arguments);
  }

  /**
   * Validates that a condition would not cause a conflict and throws CONFLICT BusinessRuleException
   * if false.
   *
   * @param condition the condition to validate (typically a uniqueness check)
   * @param message the error message (supports String.format placeholders)
   * @param arguments the arguments for the message format
   * @throws BusinessRuleException with CONFLICT_GENERAL error code if condition is false
   */
  public static void isConflict(
      final boolean condition, final String message, final Object... arguments) {
    isValid(condition, CONFLICT_GENERAL, message, arguments);
  }

  /**
   * Trims and validates an ID value using the default maximum size.
   *
   * @param idValue the ID value to validate
   * @param idName the name of the ID field (used in error messages)
   * @return the trimmed ID value
   * @throws BusinessRuleException if ID is null, empty, or invalid
   */
  @NotEmpty
  public static String trimAndValidateId(final String idValue, final String idName) {
    return trimAndValidateId(idValue, idName, DEFAULT_MAX_ID_SIZE);
  }

  /**
   * Trims and validates an ID value with a specified maximum size. The ID must be non-empty and
   * must match valid ID format.
   *
   * @param idValue the ID value to validate
   * @param idName the name of the ID field (used in error messages)
   * @param maxIdSize the maximum allowed size for the ID
   * @return the trimmed ID value
   * @throws BusinessRuleException if ID is null, empty, or invalid
   * @throws IllegalArgumentException if idName is empty or maxIdSize is not positive
   */
  @NotEmpty
  public static String trimAndValidateId(String idValue, String idName, final int maxIdSize) {
    idName = trimToEmpty(idName);
    isTrue(isNotEmpty(idName), "Expected not empty idName");
    isTrue(maxIdSize > 0, "Expected maxIdSize to be positive: %s", maxIdSize);

    idValue = trimToEmpty(idValue);

    isValidArgument(isNotEmpty(idValue), "Expected not empty %s", idName);

    isValidArgument(
        isValidId(idValue, maxIdSize),
        "Invalid %s: %s",
        idName,
        hideString(idValue, maxIdSize / 2));

    return idValue;
  }

  /**
   * Converts an HTTP error response to a BusinessRuleException using the default object serializer.
   *
   * @param httpStatus the HTTP status code
   * @param errorResponse the error response body (JSON format)
   * @return BusinessRuleException representing the error
   */
  @NotNull
  public static BusinessRuleException toBusinessRuleException(
      final int httpStatus, final String errorResponse) {
    return toBusinessRuleException(httpStatus, errorResponse, defaultObjectSerializer);
  }

  /**
   * Converts an HTTP error response to a BusinessRuleException using a custom object serializer.
   * This method parses the error response and extracts error code, message, and details.
   *
   * @param httpStatus the HTTP status code
   * @param errorResponse the error response body (JSON format)
   * @param objectSerializer the serializer to use for parsing the response
   * @return BusinessRuleException representing the error
   */
  @NotNull
  public static BusinessRuleException toBusinessRuleException(
      final int httpStatus, String errorResponse, final ObjectSerializer objectSerializer) {

    errorResponse = trimToEmpty(errorResponse);

    final Map<String, Object> errorData =
        isNotEmpty(errorResponse)
            ? objectSerializer.toObjectSuppressLog(
                errorResponse, ClassTypeProvider.of(new TypeReference<>() {}))
            : null;

    ErrorDto errorDto =
        isNotEmpty(errorData)
                && errorData.containsKey("errorCode")
                && errorData.containsKey("httpStatus")
            ? objectSerializer.toObjectSuppressLog(
                errorResponse, ClassTypeProvider.of(ErrorDto.class))
            : null;

    if (errorDto == null && errorData != null) {
      final ErrorCode errorCode = GeneralErrorCode.errorCodeFromHttpStatus(httpStatus);

      if (errorData.size() == 1
          && (errorData.containsKey(JSON_RESPONSE_MESSAGE_KEY)
              || errorData.containsKey(JSON_RESPONSE_ERROR_KEY))) {
        final Object errorMsg =
            errorData.containsKey(JSON_RESPONSE_MESSAGE_KEY)
                ? errorData.get(JSON_RESPONSE_MESSAGE_KEY)
                : errorData.get(JSON_RESPONSE_ERROR_KEY);

        errorDto =
            ErrorDto.builder()
                .httpStatus(httpStatus)
                .errorCode(errorCode.errorCode())
                .errorMessage(errorCode.errorMessage())
                .errorDetails(List.of(errorMsg.toString()))
                .build();
      } else if (errorData.size() == 2
          && errorData.containsKey(JSON_RESPONSE_MESSAGE_KEY)
          && errorData.containsKey(JSON_RESPONSE_ERROR_KEY)) {

        final String message = errorData.get(JSON_RESPONSE_MESSAGE_KEY).toString();
        final String error = errorData.get(JSON_RESPONSE_ERROR_KEY).toString();

        errorDto =
            ErrorDto.builder()
                .httpStatus(httpStatus)
                .errorCode(errorCode.errorCode())
                .errorMessage(errorCode.errorMessage())
                .errorDetails(List.of(error, message))
                .build();
      }
    }

    if (errorDto == null) {
      return toBusinessRuleException(
          ErrorDto.builder().httpStatus(httpStatus).build(), errorResponse);
    }

    if (errorDto.getHttpStatus() == 0) {
      errorDto = errorDto.toBuilder().httpStatus(httpStatus).build();
    }

    return toBusinessRuleException(errorDto);
  }

  /**
   * Converts an ErrorDto to a BusinessRuleException.
   *
   * @param errorDto the error DTO containing error information
   * @return BusinessRuleException representing the error
   */
  @NotNull
  public static BusinessRuleException toBusinessRuleException(final ErrorDto errorDto) {
    return toBusinessRuleException(errorDto, null);
  }

  /**
   * Converts an ErrorDto to a BusinessRuleException with an optional custom error message. This
   * method handles error code mapping, HTTP status validation, and preserves error codes from
   * external systems when appropriate.
   *
   * @param errorDto the error DTO containing error information
   * @param errorMessage optional custom error message to override the DTO's message
   * @return BusinessRuleException representing the error
   */
  @NotNull
  public static BusinessRuleException toBusinessRuleException(
      final ErrorDto errorDto, final String errorMessage) {

    if (errorDto == null) {
      return new BusinessRuleException(
          INTERNAL_SERVER_ERROR_GENERAL,
          "%s",
          requireNonNullElse(trimToNull(errorMessage), "Unknown"));
    }

    final ErrorCode errorCodeFromHttpStatus =
        errorDto.getHttpStatus() != 0
            ? GeneralErrorCode.from(
                (errorDto.getHttpStatus() * HTTP_STATUS_ERROR_CODE_BASE) + 9999, false)
            : INTERNAL_SERVER_ERROR_GENERAL;

    ErrorCode errorCode;

    if (errorDto.getErrorCode() == 0) {
      errorCode = errorCodeFromHttpStatus;

    } else {
      errorCode = GeneralErrorCode.from(errorDto.getErrorCode(), false);

      // To guard ourselves if someone else uses the same structure with different error codes,
      // we rely on the http status to be the same as the one from the error code.
      final int httpsStatusFromErrorCode = httpStatusFromValue(errorCode.errorCode());

      if (errorDto.getHttpStatus() > 0 && errorDto.getHttpStatus() != httpsStatusFromErrorCode) {
        errorCode = errorCodeFromHttpStatus;
      }
    }

    // Preserve the original error code from external systems when it differs from our enum
    // and represents a valid HTTP-status-based error code (> 599).
    // This ensures proper error propagation from external services using custom error codes.
    if (errorCode.errorCode() != errorDto.getErrorCode()
        && errorDto.getErrorCode() > HTTP_STATUS_MAX_VALUE
        && httpStatusFromValueCheck(errorDto.getErrorCode(), false) != null) {

      errorCode = createErrorCodeFromDto(errorDto);
    }

    String message = "";
    if (isNotEmpty(errorMessage)) {
      message = errorMessage;
    } else if (errorDto.getErrorDetails() != null) {
      message = String.join(", ", errorDto.getErrorDetails());
    }

    return new BusinessRuleException(
        errorDto.getCause(),
        errorDto.getTimestamp() != null ? errorDto.getTimestamp() : Instant.now(),
        errorCode,
        "%s",
        message);
  }

  /**
   * Extracts or wraps an exception as a BusinessRuleException with INTERNAL_SERVER_ERROR error
   * code. If the exception or its causes already contain a BusinessRuleException, it is returned
   * directly.
   *
   * @param e the exception to extract from
   * @param errorMessage the error message to use if creating a new BusinessRuleException (supports
   *     String.format placeholders)
   * @param args the arguments for the error message format
   * @return the extracted or newly created BusinessRuleException
   */
  public static BusinessRuleException extractBusinessRuleException(
      final Exception e, final String errorMessage, final Object... args) {

    return extractBusinessRuleException(e, INTERNAL_SERVER_ERROR_GENERAL, errorMessage, args);
  }

  /**
   * Extracts or wraps an exception as a BusinessRuleException with a specified error code. If the
   * exception or its causes already contain a BusinessRuleException, it is returned directly.
   * Otherwise, creates a new BusinessRuleException wrapping the original exception.
   *
   * @param e the exception to extract from
   * @param errorCode the error code to use if creating a new BusinessRuleException
   * @param errorMessage the error message to use if creating a new BusinessRuleException (supports
   *     String.format placeholders)
   * @param args the arguments for the error message format
   * @return the extracted or newly created BusinessRuleException
   */
  public static BusinessRuleException extractBusinessRuleException(
      final Exception e, ErrorCode errorCode, final String errorMessage, final Object... args) {

    requireNonNull(e, "Exception is required");
    notEmpty(errorMessage, "Error message is required");

    errorCode = errorCode != null ? errorCode : INTERNAL_SERVER_ERROR_GENERAL;

    if (e instanceof final BusinessRuleException bre) {
      return bre;
    }

    if (e.getCause() instanceof final BusinessRuleException bre) {
      return bre;
    }

    if (e.getCause() != null
        && e.getCause().getCause() instanceof final BusinessRuleException bre) {
      return bre;
    }

    return new BusinessRuleException(e, errorCode, errorMessage, args);
  }

  /**
   * Creates an ErrorCode wrapper that preserves the original error code from an external ErrorDto.
   * This is used when external systems send error codes that don't match our GeneralErrorCode enum,
   * but we still want to preserve their error code value for proper error propagation.
   *
   * @param errorDto the error DTO containing the custom error code
   * @return ErrorCode instance wrapping the DTO's error code and message
   */
  public static ErrorCode createErrorCodeFromDto(final ErrorDto errorDto) {
    return new ErrorCode() {
      @Override
      public int errorCode() {
        return errorDto.getErrorCode();
      }

      @Override
      public String errorMessage() {
        return errorDto.getErrorMessage();
      }

      @Override
      public String toString() {
        return isNotEmpty(errorDto.getErrorMessage())
            ? errorDto.getErrorMessage()
            : String.valueOf(errorDto.getErrorCode());
      }
    };
  }
}
