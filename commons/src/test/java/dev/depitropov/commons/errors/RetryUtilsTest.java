package dev.depitropov.commons.errors;

import static dev.depitropov.commons.errors.GeneralErrorCode.INSUFFICIENT_STORAGE_GENERAL;
import static dev.depitropov.commons.errors.GeneralErrorCode.INTERNAL_SERVER_ERROR_GENERAL;
import static dev.depitropov.commons.errors.GeneralErrorCode.TOO_MANY_REQUESTS_GENERAL;
import static dev.depitropov.commons.errors.RetryUtils.DEFAULT_RETRY_CONFIG;
import static dev.depitropov.commons.errors.RetryUtils.DEFAULT_RETRY_EXCEPTION_MESSAGES;
import static dev.depitropov.commons.errors.RetryUtils.doRetryDelay;
import static dev.depitropov.commons.errors.RetryUtils.executeWithRetry;
import static dev.depitropov.commons.errors.RetryUtils.isRetryableException;
import static dev.depitropov.commons.errors.RetryUtils.nextRetryDelayMs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.blindpirate.extensions.CaptureSystemOutput;
import com.github.blindpirate.extensions.CaptureSystemOutput.OutputCapture;
import dev.depitropov.commons.errors.RetryUtils.IsRetryable;
import dev.depitropov.commons.errors.RetryUtils.RetryExecutionException;
import dev.depitropov.commons.errors.RetryUtils.RetryExhaustedException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLHandshakeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Test;

@CaptureSystemOutput
@Slf4j
class RetryUtilsTest {

  @Test
  void testExecuteWithRetry(final OutputCapture output) {
    final AtomicInteger iter = new AtomicInteger(0);
    executeWithRetry(
        () -> {
          log.info("Executed test iter: {}", iter);

          iter.incrementAndGet();

          if (iter.get() < 2) {
            throw new RuntimeException("Connection reset");
          }

          if (iter.get() < 4) {
            throw new RuntimeException(new RuntimeException("Connection refused"));
          }
        });
    assertThat(iter.get()).isEqualTo(4);

    assertThat(output.toString())
        .containsSubsequence(
            "Executed test iter: 0",
            "Executed test iter: 1",
            "Executed test iter: 2",
            "Executed test iter: 3");
    assertThat(output.toString())
        .containsSubsequence(
            "Retry 1 on error: Connection reset",
            "Retry 2 on error: java.lang.RuntimeException: Connection refused",
            "Retry 3 on error: java.lang.RuntimeException: Connection refused");
  }

  @Test
  void testExecuteWithRetryReturn(final OutputCapture output) {
    final AtomicInteger iter = new AtomicInteger(0);
    final String res =
        executeWithRetry(
            () -> {
              log.info("Executed test iter: {}", iter);

              iter.incrementAndGet();

              if (iter.get() < 2) {
                throw new RuntimeException("service unavailable");
              }

              if (iter.get() < 3) {
                throw new RuntimeException("HTTP 502 Bad Gateway");
              }

              if (iter.get() < 4) {
                throw new RuntimeException(new RuntimeException("connection prematurely closed"));
              }

              return "test";
            });

    assertThat(res).isEqualTo("test");
    assertThat(iter.get()).isEqualTo(4);

    assertThat(output.toString())
        .containsSubsequence(
            "Retry 1 on error: service unavailable",
            "Retry 2 on error: HTTP 502 Bad Gateway",
            "Retry 3 on error: java.lang.RuntimeException: connection prematurely closed");
  }

  @Test
  void testExecuteWithRetryVoidWithConfig(final OutputCapture output) {
    final ArrayList<String> customRetryExceptionMsgs =
        new ArrayList<>(DEFAULT_RETRY_EXCEPTION_MESSAGES);
    customRetryExceptionMsgs.add("custom message");

    final AtomicInteger iter = new AtomicInteger(0);

    executeWithRetry(
        () -> {
          log.info("Executed test iter: {}", iter);

          iter.incrementAndGet();

          if (iter.get() < 2) {
            throw new RuntimeException("custom message");
          }

          if (iter.get() < 4) {
            throw new RuntimeException(new SSLHandshakeException("test"));
          }
        },
        RetryConfig.builder()
            .minFixedDelayMs(10)
            .maxFixedDelayMs(200)
            .backoffPerRetryDelayMs(10)
            .retryOnExceptionMessages(customRetryExceptionMsgs)
            .build());

    assertThat(iter.get()).isEqualTo(4);

    assertThat(output.toString())
        .containsSubsequence(
            "Retry 1 on error: custom message",
            "Retry 2 on error: javax.net.ssl.SSLHandshakeException: test",
            "Retry 3 on error: javax.net.ssl.SSLHandshakeException: test");
  }

  @Test
  void testExecuteWithRetryReturnWithConfig(final OutputCapture output) {
    final AtomicInteger iter = new AtomicInteger(0);

    final String res =
        executeWithRetry(
            () -> {
              log.info("Executed test iter: {}", iter);

              iter.incrementAndGet();

              if (iter.get() < 4) {
                throw new RuntimeException(new SSLHandshakeException("test"));
              }

              return "test";
            },
            RetryConfig.builder()
                .logPrefix("[REQUEST-1] ")
                .maxRetries(-1)
                .maxFixedDelayMs(10)
                .minFixedDelayMs(200)
                .backoffPerRetryDelayMs(-20)
                .logWarningOnExecutionTimeMs(10)
                .build());

    assertThat(res).isEqualTo("test");
    assertThat(iter.get()).isEqualTo(4);

    assertThat(output.toString())
        .containsSubsequence(
            "DEBUG dev.depitropov.commons.errors.RetryUtils - [REQUEST-1] Retry after ",
            "ERROR dev.depitropov.commons.errors.RetryUtils - [REQUEST-1] Retry 1 on error: javax.net.ssl.SSLHandshakeException: test",
            "WARN dev.depitropov.commons.errors.RetryUtils - [REQUEST-1] Executed request take too long time:",
            "ERROR dev.depitropov.commons.errors.RetryUtils - [REQUEST-1] Retry 2 on error: javax.net.ssl.SSLHandshakeException: test",
            "ERROR dev.depitropov.commons.errors.RetryUtils - [REQUEST-1] Retry 3 on error: javax.net.ssl.SSLHandshakeException: test");
  }

  @Test
  void testExecuteWithRetryReturnWithCustomExceptionClass(final OutputCapture output) {
    final AtomicInteger iter = new AtomicInteger(0);

    final String res =
        executeWithRetry(
            () -> {
              log.info("Executed test iter: {}", iter);

              iter.incrementAndGet();

              if (iter.get() < 2) {
                throw new RuntimeException(new IllegalAccessError("test"));
              }

              return "test";
            },
            RetryConfig.builder()
                .retryOnExceptionClasses(List.of(IllegalAccessError.class))
                .build());

    assertThat(res).isEqualTo("test");
    assertThat(iter.get()).isEqualTo(2);

    assertThat(output.toString())
        .containsSubsequence(
            "INFO dev.depitropov.commons.errors.RetryUtilsTest - Executed test iter: 0",
            "ERROR dev.depitropov.commons.errors.RetryUtils - Retry 1 on error: java.lang.IllegalAccessError: test",
            "INFO dev.depitropov.commons.errors.RetryUtilsTest - Executed test iter: 1");
  }

  @Test
  void testThrowsNotRetryableException(final OutputCapture output) {
    assertThat(
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                        executeWithRetry(
                            () -> {
                              throw new IllegalArgumentException("test exception");
                            }))
                .getMessage())
        .isEqualTo("test exception");

    // Asserted on substance, not on the logging backend's line format: the call ran
    // once, reported its duration, and never retried.
    assertThat(output.toString())
        .containsPattern(
            "DEBUG dev\\.depitropov\\.commons\\.errors\\.RetryUtils - Executed request time: \\d+ ms")
        .doesNotContain("Retry 1 on error");
  }

  @Test
  void testThrowsRetryExhaustedException(final OutputCapture output) {
    final RetryConfig config =
        RetryConfig.builder()
            .maxRetries(2)
            .minFixedDelayMs(-1)
            .maxFixedDelayMs(-1)
            .backoffPerRetryDelayMs(-1)
            .build();

    assertThat(
            assertThrows(
                    RetryExhaustedException.class,
                    () ->
                        executeWithRetry(
                            () -> {
                              throw new RuntimeException("connection reset");
                            },
                            config))
                .getMessage())
        .isEqualTo("Reties exhausted: 2");

    assertThat(output.toString())
        .containsSubsequence(
            "ERROR dev.depitropov.commons.errors.RetryUtils - Retry 1 on error: connection reset",
            "ERROR dev.depitropov.commons.errors.RetryUtils - Retry 2 on error: connection reset");
  }

  @Test
  void testCustomLogger(final OutputCapture output) {
    final AtomicInteger iter = new AtomicInteger(0);

    final String res =
        executeWithRetry(
            () -> {
              log.info("Executed test iter: {}", iter);

              iter.incrementAndGet();

              if (iter.get() < 2) {
                throw new RuntimeException("err_connection_timed_out");
              }

              return "test";
            },
            RetryConfig.builder()
                .minFixedDelayMs(1)
                .maxFixedDelayMs(1)
                .backoffPerRetryDelayMs(1)
                .log(log)
                .build());

    assertThat(res).isEqualTo("test");
    assertThat(iter.get()).isEqualTo(2);

    assertThat(output.toString())
        .containsSubsequence(
            "INFO dev.depitropov.commons.errors.RetryUtilsTest - Executed test iter: 0",
            "INFO dev.depitropov.commons.errors.RetryUtilsTest - Executed test iter: 1");
    assertThat(output.toString())
        .containsSubsequence(
            "ERROR dev.depitropov.commons.errors.RetryUtilsTest - Retry 1 on error: err_connection_timed_out");
  }

  @Test
  void testNextRetryDelayMs() {
    for (int j = 0; j < 1000; j++) {
      for (int i = 0; i < 10; i++) {
        final long delayMs = nextRetryDelayMs(i);

        assertThat(delayMs)
            .isBetween(
                (long) DEFAULT_RETRY_CONFIG.minFixedDelayMs(),
                (long) DEFAULT_RETRY_CONFIG.maxFixedDelayMs()
                    + i
                        * (DEFAULT_RETRY_CONFIG.minFixedDelayMs()
                            + (long) i * DEFAULT_RETRY_CONFIG.backoffPerRetryDelayMs()));
      }
    }
  }

  @Test
  void testNextRetryDelayMsWithCustomConfig() {
    final RetryConfig config =
        RetryConfig.builder()
            .minFixedDelayMs(10)
            .maxFixedDelayMs(100)
            .backoffPerRetryDelayMs(10)
            .build();

    for (int j = 0; j < 1000; j++) {
      for (int i = 0; i < 10; i++) {
        final long delayMs = nextRetryDelayMs(i, config);

        assertThat(delayMs)
            .isBetween(
                (long) config.minFixedDelayMs(),
                (long) config.maxFixedDelayMs()
                    + i * (config.minFixedDelayMs() + (long) i * config.backoffPerRetryDelayMs()));
      }
    }
  }

  @Test
  void testDoRetryDelay() {
    final RetryConfig config =
        RetryConfig.builder()
            .minFixedDelayMs(10)
            .maxFixedDelayMs(100)
            .backoffPerRetryDelayMs(10)
            .build();

    final StopWatch stopWatch = StopWatch.createStarted();

    doRetryDelay(0);

    // For iteration 0 the delay is nextLong(minFixedDelayMs, maxFixedDelayMs), so the guaranteed
    // lower bound is the min-fixed-delay-ms configured via the pom.xml system properties.
    assertThat(stopWatch.getTime(TimeUnit.MILLISECONDS))
        .isGreaterThanOrEqualTo(DEFAULT_RETRY_CONFIG.minFixedDelayMs());

    stopWatch.reset();
    stopWatch.start();

    doRetryDelay(0, config);

    assertThat(stopWatch.getTime(TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(10L);
  }

  @Test
  void testIsRetryableException() {
    DEFAULT_RETRY_CONFIG
        .retryOnExceptionMessages()
        .forEach(msg -> assertThat(isRetryableException(new RuntimeException(msg))).isTrue());

    assertThat(isRetryableException(new SSLHandshakeException("test"))).isTrue();
    assertThat(isRetryableException(new RuntimeException("test"))).isFalse();

    assertThat(
            isRetryableException(
                new RuntimeException("test"),
                RetryConfig.builder().retryOnExceptionMessages(List.of("test")).build()))
        .isTrue();

    assertThat(
            isRetryableException(
                new RuntimeException("test space"),
                RetryConfig.builder().retryOnExceptionMessages(List.of("Test_space")).build()))
        .isTrue();

    assertThat(
            isRetryableException(
                new Throwable("test space"),
                RetryConfig.builder().retryOnExceptionMessages(List.of("Test_space")).build()))
        .isTrue();

    assertThat(
            isRetryableException(
                new Error("test space"),
                RetryConfig.builder().retryOnExceptionMessages(List.of("Test_space")).build()))
        .isTrue();

    assertThat(
            isRetryableException(
                new RuntimeException(),
                RetryConfig.builder()
                    .retryOnExceptionClassNames(List.of("runtimeEXCEPTIOn"))
                    .build()))
        .isTrue();

    assertThat(
            isRetryableException(
                new RuntimeException(),
                RetryConfig.builder()
                    .retryOnExceptionClassNames(List.of("runtimeEXCEPTIOnUnknown"))
                    .build()))
        .isFalse();

    assertThat(
            isRetryableException(
                new IllegalArgumentException(),
                RetryConfig.builder()
                    .retryOnExceptionClasses(List.of(IllegalArgumentException.class))
                    .build()))
        .isTrue();

    assertThat(
            isRetryableException(
                new IllegalArgumentException("custom"),
                RetryConfig.builder()
                    .retryOnExceptions(
                        throwables ->
                            throwables.stream()
                                .anyMatch(
                                    iter ->
                                        iter instanceof IllegalArgumentException
                                            && iter.getMessage().equals("custom")))
                    .build()))
        .isTrue();
  }

  @Test
  void testIsRetryableException_OnEmptyConfigDefaultsAreNotUsed() {
    DEFAULT_RETRY_EXCEPTION_MESSAGES.forEach(
        msg ->
            assertThat(
                    isRetryableException(
                        new IllegalArgumentException(msg),
                        RetryConfig.builder().retryOnExceptionMessages(List.of()).build()))
                .isFalse());

    assertThat(
            isRetryableException(
                new SSLHandshakeException("test"),
                RetryConfig.builder().retryOnExceptionClasses(List.of()).build()))
        .isFalse();
  }

  @Test
  void retryWithCheckedException() {
    final RetryConfig config =
        RetryConfig.builder()
            .minFixedDelayMs(10)
            .maxFixedDelayMs(100)
            .backoffPerRetryDelayMs(10)
            .retryOnExceptions(
                (throwables -> throwables.stream().anyMatch(iter -> iter instanceof IOException)))
            .build();

    assertThrows(
        RetryExhaustedException.class,
        () -> executeWithRetry(this::checkedExceptionMethod, config));
  }

  @Test
  void retryWithExceptionClassName() {
    final RetryConfig config =
        RetryConfig.builder()
            .minFixedDelayMs(10)
            .maxFixedDelayMs(100)
            .backoffPerRetryDelayMs(10)
            .retryOnExceptionClassNames(
                List.of("java.lang.illegalMonitorStateException", "NativeIoException"))
            .build();

    assertThrows(
        RetryExhaustedException.class,
        () ->
            executeWithRetry(
                () -> {
                  throw new IllegalMonitorStateException();
                },
                config));

    assertThrows(
        RetryExhaustedException.class,
        () ->
            executeWithRetry(
                () -> {
                  throw new Error(new IllegalMonitorStateException());
                },
                config));

    assertThrows(
        RetryExhaustedException.class,
        () ->
            executeWithRetry(
                () -> {
                  throw new RuntimeException("custom", new IllegalMonitorStateException());
                },
                config));

    assertThrows(
        RetryExhaustedException.class,
        () ->
            executeWithRetry(
                () -> {
                  throw new MyNativeIoException("custom");
                },
                config));

    final RetryConfig configDefault =
        RetryConfig.builder()
            .minFixedDelayMs(10)
            .maxFixedDelayMs(100)
            .backoffPerRetryDelayMs(10)
            .build();

    assertThrows(
        RetryExhaustedException.class,
        () ->
            executeWithRetry(
                () -> {
                  throw new MyNativeIoException("custom");
                },
                configDefault));

    final RetryConfig configDisabled =
        RetryConfig.builder()
            .minFixedDelayMs(10)
            .maxFixedDelayMs(100)
            .backoffPerRetryDelayMs(10)
            .retryOnExceptionClassNames(List.of())
            .build();

    assertThatThrownBy(
            () ->
                executeWithRetry(
                    () -> {
                      throw new MyNativeIoException("custom");
                    },
                    configDisabled))
        .isInstanceOf(RetryExecutionException.class)
        .hasCauseInstanceOf(MyNativeIoException.class);
  }

  @Test
  void retryWithBusinessRuleException() {
    final RetryConfig config =
        RetryConfig.builder()
            .minFixedDelayMs(10)
            .maxFixedDelayMs(100)
            .backoffPerRetryDelayMs(10)
            .build();

    assertThrows(
        RetryExhaustedException.class,
        () ->
            executeWithRetry(
                () -> {
                  throw new BusinessRuleException(INTERNAL_SERVER_ERROR_GENERAL, "Internal");
                },
                config));

    assertThrows(
        RetryExhaustedException.class,
        () ->
            executeWithRetry(
                () -> {
                  throw new RuntimeException(
                      new BusinessRuleException(TOO_MANY_REQUESTS_GENERAL, "Internal"));
                },
                config));

    assertThrows(
        RetryExhaustedException.class,
        () ->
            executeWithRetry(
                () -> {
                  throw new BusinessRuleException(INSUFFICIENT_STORAGE_GENERAL, "Internal");
                },
                config));

    final RetryConfig configDisabled =
        RetryConfig.builder()
            .minFixedDelayMs(10)
            .maxFixedDelayMs(100)
            .backoffPerRetryDelayMs(10)
            .retryOnBusinessRuleExceptions(false)
            .build();

    assertThrows(
        BusinessRuleException.class,
        () ->
            executeWithRetry(
                () -> {
                  throw new BusinessRuleException(INSUFFICIENT_STORAGE_GENERAL, "Internal");
                },
                configDisabled));
  }

  @Test
  void isRetryPredicate() {
    assertThat(new IsRetryable().test(new SSLHandshakeException("test"))).isTrue();
    assertThat(new IsRetryable().test(new RuntimeException("test"))).isFalse();

    class IsRetryableCustom extends IsRetryable {
      @Override
      protected RetryConfig getRetryConfig() {
        return super.getRetryConfig().toBuilder().retryOnExceptionMessages(List.of("test")).build();
      }
    }

    assertThat(new IsRetryableCustom().test(new RuntimeException("test"))).isTrue();
    assertThat(new IsRetryableCustom().test(new RuntimeException("not"))).isFalse();
  }

  static class MyNativeIoException extends IOException {
    public MyNativeIoException(final String message) {
      super(message);
    }
  }

  private void checkedExceptionMethod() throws IOException {
    throw new IOException("test");
  }
}
