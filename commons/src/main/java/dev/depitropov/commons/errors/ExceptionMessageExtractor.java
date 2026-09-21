package dev.depitropov.commons.errors;

/**
 * Interface for extracting error codes and messages from specific exception types. Allows
 * customization of how exceptions are interpreted and converted into structured error information.
 *
 * <p>Implementations are registered with {@link
 * ErrorUtils#registerExceptionMessageExtractors(java.util.List)} and are used automatically when
 * processing exceptions in the error handling utilities.
 *
 * @param <T> the type of throwable this extractor handles
 */
public interface ExceptionMessageExtractor<T extends Throwable> {

  /**
   * Returns the exception class that this extractor handles.
   *
   * @return the exception class
   */
  Class<T> getExceptionClass();

  /**
   * Extracts an error code from the given throwable.
   *
   * @param throwable the exception to extract error code from
   * @return the extracted error code
   */
  ErrorCode getErrorCode(Throwable throwable);

  /**
   * Extracts an error message from the given throwable.
   *
   * @param throwable the exception to extract message from
   * @return the extracted error message
   */
  String getMessage(Throwable throwable);
}
