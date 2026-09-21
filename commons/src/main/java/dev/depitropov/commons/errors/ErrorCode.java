package dev.depitropov.commons.errors;

import java.io.Serializable;

/**
 * Interface representing an error code with associated metadata. Provides a standard contract for
 * all error codes used throughout the application, ensuring consistency in error handling and
 * reporting.
 *
 * <p>Implementations typically include enums like {@link GeneralErrorCode} that define standard
 * HTTP-based error codes, as well as dynamic error codes created from external systems.
 */
public interface ErrorCode extends Serializable {

  /**
   * Returns the numeric error code value. This is typically based on HTTP status codes multiplied
   * by a base value.
   *
   * @return the error code as an integer
   */
  int errorCode();

  /**
   * Returns a human-readable error message associated with this error code.
   *
   * @return the error message
   */
  String errorMessage();
}
