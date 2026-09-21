package dev.depitropov.commons.errors;

import static dev.depitropov.commons.DEObjectUtils.getDefaultIfNull;
import static dev.depitropov.commons.errors.GeneralErrorCode.httpStatusFromValue;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.exception.ExceptionUtils.getThrowableList;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.net.ssl.SSLHandshakeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;

/**
 * Utility class for executing operations with automatic retry logic and exponential backoff.
 * Provides comprehensive retry capabilities with configurable delays, backoff strategies, and
 * customizable retry conditions based on exception types, messages, and HTTP status codes.
 *
 * <p>Default retry behavior includes:
 *
 * <ul>
 *   <li>Maximum of 5 retry attempts
 *   <li>Random delay between 250ms and 1750ms per retry
 *   <li>Exponential backoff with 250ms increment per retry
 *   <li>Automatic retry on common network and server errors (5xx, 429, connection issues)
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Simple retry with defaults
 * String result = RetryUtils.executeWithRetry(() -> externalService.call());
 *
 * // Custom retry configuration
 * RetryConfig config = RetryConfig.builder()
 *     .maxRetries(3)
 *     .minFixedDelayMs(100)
 *     .build();
 * RetryUtils.executeWithRetry(() -> externalService.call(), config);
 * }</pre>
 */
@SuppressWarnings("java:S112")
@Slf4j
public final class RetryUtils {

  /** Default maximum number of retry attempts. */
  public static final int DEFAULT_MAX_RETRIES =
      Integer.getInteger("config.commons.retry-utils.max-retries", 5);

  /** Default minimum fixed delay in milliseconds before retrying. */
  public static final int DEFAULT_MIN_FIXED_DELAY_MS =
      Integer.getInteger("config.commons.retry-utils.min-fixed-delay-ms", 250);

  /** Default maximum fixed delay in milliseconds before retrying. */
  public static final int DEFAULT_MAX_FIXED_DELAY_MS =
      Integer.getInteger("config.commons.retry-utils.max-fixed-delay-ms", 1750);

  /** Default backoff delay increment per retry iteration in milliseconds. */
  public static final int DEFAULT_BACKOFF_PER_RETRY_DELAY_MS =
      Integer.getInteger("config.commons.retry-utils.backoff-per-retry-delay-ms", 250);

  /** Default exception classes that trigger automatic retry. */
  public static final List<Class<?>> DEFAULT_RETRY_EXCEPTION_CLASSES =
      List.of(SSLHandshakeException.class);

  /** Default exception class names (case-insensitive) that trigger automatic retry. */
  public static final List<String> DEFAULT_RETRY_EXCEPTION_CLASS_NAMES =
      Stream.of(
              "NoHttpResponseException",
              "NativeIoException",
              "PrematureCloseException",
              "HttpClosedException",
              "RateLimitException",
              "NoRouteToHostException",
              "timeout")
          .map(String::toLowerCase)
          .toList();

  /**
   * Default exception messages (case-insensitive) that trigger automatic retry. Note: Messages are
   * stored lowercase and underscores are converted to spaces when comparing.
   */
  public static final List<String> DEFAULT_RETRY_EXCEPTION_MESSAGES =
      List.of(
          "502 server error",
          "bad gateway",
          "connection has been closed",
          "connection prematurely closed",
          "connection refused",
          "connection reset",
          "connection timed out",
          "connection was closed",
          "err connection timed out",
          "internal server error",
          "retry the request",
          "token is expired",
          "service unavailable",
          "please reduce your request rate",
          "pending acquire queue has reached its maximum size",
          "[503] cannot fetch access token",
          "stream was closed",
          "too many requests");

  /** Default retry configuration with standard settings. */
  public static final RetryConfig DEFAULT_RETRY_CONFIG =
      RetryConfig.builder()
          .maxRetries(DEFAULT_MAX_RETRIES)
          .minFixedDelayMs(DEFAULT_MIN_FIXED_DELAY_MS)
          .maxFixedDelayMs(DEFAULT_MAX_FIXED_DELAY_MS)
          .backoffPerRetryDelayMs(DEFAULT_BACKOFF_PER_RETRY_DELAY_MS)
          .retryOnExceptionClasses(DEFAULT_RETRY_EXCEPTION_CLASSES)
          .retryOnExceptionClassNames(DEFAULT_RETRY_EXCEPTION_CLASS_NAMES)
          .retryOnExceptionMessages(DEFAULT_RETRY_EXCEPTION_MESSAGES)
          .retryOnBusinessRuleExceptions(true)
          .logRetryAsWarning(false)
          .build();

  /** HTTP status code 429 - Too Many Requests. */
  public static final int HTTP_STATUS_TOO_MANY_REQUESTS = 429;

  /**
   * Exception thrown when all retry attempts have been exhausted without success. Contains the
   * original exception that caused the retries and information about retry attempts.
   */
  public static class RetryExhaustedException extends RuntimeException {
    /**
     * Creates a RetryExhaustedException with a message and the original cause.
     *
     * @param message the error message indicating retry exhaustion
     * @param cause the last exception that caused the final retry failure
     */
    public RetryExhaustedException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Exception thrown when an unexpected checked exception occurs during retry execution. This wraps
   * checked exceptions that are not retryable into a runtime exception.
   */
  public static class RetryExecutionException extends RuntimeException {
    /**
     * Creates a RetryExecutionException wrapping the original exception.
     *
     * @param cause the exception that occurred during execution
     */
    public RetryExecutionException(final Throwable cause) {
      super(cause);
    }
  }

  /**
   * Functional interface for operations that don't return a value but may throw exceptions. Used
   * with {@link #executeWithRetry(RunnableException)} for retry logic.
   */
  public interface RunnableException {
    /**
     * Executes the operation.
     *
     * @throws Exception if the operation fails
     */
    void execute() throws Exception;
  }

  /**
   * Functional interface for operations that return a value and may throw exceptions. Used with
   * {@link #executeWithRetry(SupplierException)} for retry logic.
   *
   * @param <T> the type of result returned by the operation
   */
  public interface SupplierException<T> {
    /**
     * Executes the operation and returns a result.
     *
     * @return the result of the operation
     * @throws Exception if the operation fails
     */
    T execute() throws Exception;
  }

  /**
   * Executes an operation with automatic retry using default configuration. The operation is
   * retried on transient failures based on default retry rules.
   *
   * @param executor the operation to execute
   * @throws RetryExhaustedException if all retry attempts are exhausted
   * @throws RetryExecutionException if a non-retryable checked exception occurs
   */
  public static void executeWithRetry(final RunnableException executor) {
    executeWithRetry(executor, DEFAULT_RETRY_CONFIG);
  }

  /**
   * Executes an operation with automatic retry using custom configuration. The operation is retried
   * based on the provided retry configuration.
   *
   * @param executor the operation to execute
   * @param retryConfig the retry configuration to use
   * @throws RetryExhaustedException if all retry attempts are exhausted
   * @throws RetryExecutionException if a non-retryable checked exception occurs
   */
  public static void executeWithRetry(
      final RunnableException executor, final RetryConfig retryConfig) {
    executeWithRetry(
        () -> {
          executor.execute();

          return null;
        },
        retryConfig);
  }

  /**
   * Executes an operation that returns a value with automatic retry using default configuration.
   * The operation is retried on transient failures based on default retry rules.
   *
   * @param <T> the type of result returned by the operation
   * @param executor the operation to execute
   * @return the result of the successful operation execution
   * @throws RetryExhaustedException if all retry attempts are exhausted
   * @throws RetryExecutionException if a non-retryable checked exception occurs
   */
  public static <T> T executeWithRetry(final SupplierException<T> executor) {
    return executeWithRetry(executor, DEFAULT_RETRY_CONFIG);
  }

  /**
   * Executes an operation that returns a value with automatic retry using custom configuration. The
   * operation is retried based on the provided retry configuration.
   *
   * @param <T> the type of result returned by the operation
   * @param executor the operation to execute
   * @param config the retry configuration to use
   * @return the result of the successful operation execution
   * @throws RetryExhaustedException if all retry attempts are exhausted
   * @throws RetryExecutionException if a non-retryable checked exception occurs
   */
  public static <T> T executeWithRetry(
      final SupplierException<T> executor, final RetryConfig config) {
    return new RetryUtilsInternal(config).executeWithRetry(executor);
  }

  /**
   * Calculates the next retry delay in milliseconds for a given iteration using default
   * configuration. The delay includes a random component and exponential backoff.
   *
   * @param iteration the current retry iteration (0-based)
   * @return the calculated delay in milliseconds before the next retry
   */
  public static long nextRetryDelayMs(final int iteration) {
    return nextRetryDelayMs(iteration, DEFAULT_RETRY_CONFIG);
  }

  /**
   * Calculates the next retry delay in milliseconds for a given iteration using custom
   * configuration. The delay includes a random component and exponential backoff based on the
   * configuration.
   *
   * @param iteration the current retry iteration (0-based)
   * @param config the retry configuration to use for calculating delay
   * @return the calculated delay in milliseconds before the next retry
   */
  public static long nextRetryDelayMs(final int iteration, final RetryConfig config) {
    return new RetryUtilsInternal(config).nextRetryDelayMs(iteration);
  }

  /**
   * Performs a retry delay for a given iteration using default configuration. This method sleeps
   * the current thread for the calculated delay period.
   *
   * @param iteration the current retry iteration (0-based)
   */
  public static void doRetryDelay(final int iteration) {
    doRetryDelay(iteration, DEFAULT_RETRY_CONFIG);
  }

  /**
   * Performs a retry delay for a given iteration using custom configuration. This method sleeps the
   * current thread for the calculated delay period.
   *
   * @param iteration the current retry iteration (0-based)
   * @param config the retry configuration to use for calculating delay
   */
  public static void doRetryDelay(final int iteration, final RetryConfig config) {
    new RetryUtilsInternal(config).doRetryDelay(iteration);
  }

  /**
   * Determines if an exception is retryable based on default configuration. An exception is
   * retryable if it matches any of the default retry conditions.
   *
   * @param throwable the exception to check
   * @return true if the exception is retryable, false otherwise
   */
  public static boolean isRetryableException(final Throwable throwable) {
    return isRetryableException(throwable, DEFAULT_RETRY_CONFIG);
  }

  /**
   * Determines if an exception is retryable based on custom configuration. An exception is
   * retryable if it matches any of the configured retry conditions.
   *
   * @param throwable the exception to check
   * @param config the retry configuration defining retry conditions
   * @return true if the exception is retryable, false otherwise
   */
  public static boolean isRetryableException(final Throwable throwable, final RetryConfig config) {
    return new RetryUtilsInternal(config).isRetryableException(throwable);
  }

  static final class RetryUtilsInternal {

    private final RetryConfig config;

    private final Logger logInstance;

    private final String logPrefix;

    private RetryUtilsInternal(final RetryConfig config) {
      this.config =
          new RetryConfig(
              getDefaultIfNull(config.maxRetries(), DEFAULT_MAX_RETRIES),
              getDefaultIfNull(config.minFixedDelayMs(), DEFAULT_MIN_FIXED_DELAY_MS),
              getDefaultIfNull(config.maxFixedDelayMs(), DEFAULT_MAX_FIXED_DELAY_MS),
              getDefaultIfNull(config.backoffPerRetryDelayMs(), DEFAULT_BACKOFF_PER_RETRY_DELAY_MS),
              config.log() != null ? config.log() : log,
              isNotEmpty(config.logPrefix()) ? config.logPrefix() : "",
              getDefaultIfNull(config.logWarningOnExecutionTimeMs(), 0),
              config.retryOnExceptionClasses() != null
                  ? config.retryOnExceptionClasses()
                  : DEFAULT_RETRY_EXCEPTION_CLASSES,
              config.retryOnExceptionClassNames() != null
                  ? config.retryOnExceptionClassNames().stream()
                      .map(msg -> trimToEmpty(msg).toLowerCase())
                      .filter(ObjectUtils::isNotEmpty)
                      .toList()
                  : DEFAULT_RETRY_EXCEPTION_CLASS_NAMES,
              config.retryOnExceptionMessages() != null
                  ? config.retryOnExceptionMessages().stream()
                      .map(msg -> trimToEmpty(msg).toLowerCase().replace('_', ' '))
                      .filter(ObjectUtils::isNotEmpty)
                      .toList()
                  : DEFAULT_RETRY_EXCEPTION_MESSAGES,
              config.retryOnBusinessRuleExceptions() != null
                  ? config.retryOnBusinessRuleExceptions()
                  : Boolean.TRUE,
              config.retryOnExceptions(),
              config.logRetryAsWarning() != null && config.logRetryAsWarning());

      logInstance = this.config.log();
      logPrefix = this.config.logPrefix();
    }

    public <T> T executeWithRetry(final SupplierException<T> executor) {

      Throwable lastException = null;

      int iterations = 0;

      final int maxRetries = config.maxRetries() <= 0 ? DEFAULT_MAX_RETRIES : config.maxRetries();

      while (iterations < maxRetries) {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
          return executor.execute();
        } catch (final Throwable t) {
          if (isRetryableException(t)) {
            lastException = t;

            doRetryDelay(iterations);

            iterations++;

            if (Boolean.TRUE.equals(config.logRetryAsWarning())) {

              logInstance.warn("{}Retry {} on error: {}", logPrefix, iterations, t.getMessage(), t);
            } else {
              logInstance.error(
                  "{}Retry {} on error: {}", logPrefix, iterations, t.getMessage(), t);
            }

            continue;
          }

          if (t instanceof final RuntimeException rte) {
            throw rte;
          }

          throw new RetryExecutionException(t);
        } finally {
          stopWatch.stop();

          final long executionTimeMs = stopWatch.getTime(MILLISECONDS);

          logInstance.debug("{}Executed request time: {} ms", logPrefix, executionTimeMs);

          if (config.logWarningOnExecutionTimeMs() > 0
              && executionTimeMs > config.logWarningOnExecutionTimeMs()) {
            logInstance.warn(
                "{}Executed request take too long time: {} ms", logPrefix, executionTimeMs);
          }
        }
      }

      throw new RetryExhaustedException(
          logPrefix + "Reties exhausted: " + maxRetries, lastException);
    }

    @SuppressWarnings("java:S2925")
    private long nextRetryDelayMs(final int iteration) {
      isTrue(iteration >= 0, "Iteration must be greater or equal to 0");

      long iterStartMs = (long) config.minFixedDelayMs() + config.backoffPerRetryDelayMs();

      iterStartMs = iterStartMs <= 0 ? 10 : iterStartMs;

      long iterEndMs =
          config.minFixedDelayMs() + (long) iteration * config.backoffPerRetryDelayMs();

      iterEndMs = iterEndMs <= iterStartMs ? iterStartMs + 1 : iterEndMs;

      final int minFixedDelayMs = config.minFixedDelayMs() <= 0 ? 10 : config.minFixedDelayMs();
      int maxFixedDelayMs = config.maxFixedDelayMs() <= 0 ? 100 : config.maxFixedDelayMs();

      maxFixedDelayMs = maxFixedDelayMs <= minFixedDelayMs ? minFixedDelayMs + 1 : maxFixedDelayMs;

      return nextLong(minFixedDelayMs, maxFixedDelayMs)
          + iteration * nextLong(iterStartMs, iterEndMs);
    }

    private void doRetryDelay(final int iteration) {
      try {
        final long sleepMs = nextRetryDelayMs(iteration);

        logInstance.debug("{}Retry after {} ms", logPrefix, sleepMs);

        Thread.sleep(sleepMs);

      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    private boolean isRetryableException(final Throwable throwable) {
      final List<Throwable> throwables = getThrowableList(throwable);

      return containsAnyExceptionClass(throwables)
          || containsAnyExceptionClassName(throwables)
          || containsAnyExceptionMessage(throwables)
          || containsBusinessRuleException(throwables)
          || customMatchThrowables(throwables);
    }

    private boolean customMatchThrowables(final List<Throwable> throwables) {
      if (config.retryOnExceptions() == null) {
        return false;
      }

      try {
        return config.retryOnExceptions().test(throwables);
      } catch (final Exception e) {

        logInstance.error("Failed to check for retryable exception, error: {}", e.getMessage(), e);
      }

      return false;
    }

    private boolean containsAnyExceptionClass(final List<Throwable> throwables) {
      if (isEmpty(config.retryOnExceptionClasses())) {
        return false;
      }

      return throwables.stream()
          .anyMatch(
              e -> config.retryOnExceptionClasses().stream().anyMatch(iter -> iter.isInstance(e)));
    }

    private boolean containsAnyExceptionClassName(final List<Throwable> throwables) {
      if (isEmpty(config.retryOnExceptionClassNames())) {
        return false;
      }

      return throwables.stream()
          .map(e -> e.getClass().getName().toLowerCase())
          .anyMatch(e -> config.retryOnExceptionClassNames().stream().anyMatch(e::contains));
    }

    private boolean containsAnyExceptionMessage(final List<Throwable> throwables) {
      if (isEmpty(config.retryOnExceptionMessages())) {
        return false;
      }

      return throwables.stream()
          .map(e -> trimToEmpty(e.getMessage()).toLowerCase().replace('_', ' '))
          .anyMatch(e -> config.retryOnExceptionMessages().stream().anyMatch(e::contains));
    }

    private boolean containsBusinessRuleException(final List<Throwable> throwables) {
      if (!Boolean.TRUE.equals(config.retryOnBusinessRuleExceptions())) {
        return false;
      }

      return throwables.stream()
          .anyMatch(
              throwable ->
                  throwable instanceof final BusinessRuleException rspExp
                      && (is5xxServerError(httpStatusFromValue(rspExp.getErrorCode().errorCode()))
                          || httpStatusFromValue(rspExp.getErrorCode().errorCode())
                              == HTTP_STATUS_TOO_MANY_REQUESTS));
    }

    private boolean is5xxServerError(final int errorCode) {
      return errorCode >= 500 && errorCode <= 599;
    }
  }

  /**
   * Predicate implementation for checking if an exception is retryable. Can be extended to provide
   * custom retry configuration.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * IsRetryable retryable = new IsRetryable();
   * if (retryable.test(exception)) {
   *   // perform retry logic
   * }
   * }</pre>
   */
  public static class IsRetryable implements Predicate<Throwable> {

    /**
     * Returns the retry configuration to use for retry decisions. Override this method to provide
     * custom retry configuration.
     *
     * @return the retry configuration (defaults to DEFAULT_RETRY_CONFIG)
     */
    protected RetryConfig getRetryConfig() {
      return DEFAULT_RETRY_CONFIG;
    }

    /**
     * Tests if the given throwable is retryable based on the retry configuration. Logs a warning if
     * the exception is retryable.
     *
     * @param t the throwable to test
     * @return true if the throwable is retryable, false otherwise
     */
    @Override
    public boolean test(final Throwable t) {
      final boolean res = isRetryableException(t, getRetryConfig());

      if (res) {
        log.warn("Retry on error: {}", t.getMessage(), t);
      }

      return res;
    }
  }
}
