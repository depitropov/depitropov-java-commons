package dev.depitropov.commons.errors;

import java.util.List;
import java.util.function.Predicate;
import lombok.Builder;
import org.slf4j.Logger;

/**
 * Configuration for retry behavior in {@link RetryUtils}. Defines retry parameters including
 * delays, backoff strategy, and conditions for retryable exceptions.
 *
 * <p>Use the builder to create custom configurations:
 *
 * <pre>{@code
 * RetryConfig config = RetryConfig.builder()
 *     .maxRetries(3)
 *     .minFixedDelayMs(100)
 *     .maxFixedDelayMs(500)
 *     .backoffPerRetryDelayMs(100)
 *     .retryOnBusinessRuleExceptions(true)
 *     .build();
 * }</pre>
 *
 * @param maxRetries maximum number of retry attempts (default: 5)
 * @param minFixedDelayMs minimum fixed delay in milliseconds before retrying (default: 250)
 * @param maxFixedDelayMs maximum fixed delay in milliseconds before retrying (default: 1750)
 * @param backoffPerRetryDelayMs backoff delay increment per retry iteration in milliseconds
 *     (default: 250)
 * @param log logger instance to use for retry logging
 * @param logPrefix prefix to add to all log messages
 * @param logWarningOnExecutionTimeMs if > 0, logs a warning when execution takes longer than this
 *     value
 * @param retryOnExceptionClasses list of exception classes that trigger retry
 * @param retryOnExceptionClassNames list of exception class name patterns (case-insensitive) that
 *     trigger retry
 * @param retryOnExceptionMessages list of exception message patterns (case-insensitive) that
 *     trigger retry
 * @param retryOnBusinessRuleExceptions if true, retries on BusinessRuleException with 5xx or 429
 *     status codes
 * @param retryOnExceptions custom predicate for determining if an exception list is retryable
 * @param logRetryAsWarning if true, logs retries as warnings instead of errors
 */
@Builder(toBuilder = true)
public record RetryConfig(
    Integer maxRetries,
    Integer minFixedDelayMs,
    Integer maxFixedDelayMs,
    Integer backoffPerRetryDelayMs,
    Logger log,
    String logPrefix,
    Integer logWarningOnExecutionTimeMs,
    List<Class<?>> retryOnExceptionClasses,
    List<String> retryOnExceptionClassNames,
    List<String> retryOnExceptionMessages,
    Boolean retryOnBusinessRuleExceptions,
    Predicate<List<Throwable>> retryOnExceptions,
    Boolean logRetryAsWarning) {}
