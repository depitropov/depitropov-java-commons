package dev.depitropov.commons.quarkus;

import static com.github.blindpirate.extensions.CaptureSystemOutput.OutputCapture;
import static dev.depitropov.commons.MdcKeys.MDC_COMPANY_ID;
import static dev.depitropov.commons.MdcKeys.MDC_LOG_PREFIX;
import static dev.depitropov.commons.MdcKeys.MDC_MSG_ID;
import static dev.depitropov.commons.MdcKeys.MDC_USER;
import static dev.depitropov.commons.MdcKeys.MDC_USER_ID;
import static dev.depitropov.commons.quarkus.QuarkusMdcUtils.executeWithMdcData;
import static dev.depitropov.commons.quarkus.QuarkusMdcUtils.executeWithMdcDataWithException;
import static dev.depitropov.commons.quarkus.QuarkusMdcUtils.executeWithMdcDataWithoutClear;
import static dev.depitropov.commons.quarkus.QuarkusMdcUtils.executeWithMdcDataWithoutClearWithException;
import static dev.depitropov.commons.quarkus.QuarkusMdcUtils.executeWithNewMdcData;
import static dev.depitropov.commons.quarkus.QuarkusMdcUtils.executeWithNewMdcDataWithException;
import static dev.depitropov.commons.quarkus.QuarkusMdcUtils.getCurrentMdcData;
import static dev.depitropov.commons.quarkus.QuarkusMdcUtils.setMdcKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.blindpirate.extensions.CaptureSystemOutput;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

@CaptureSystemOutput
@Slf4j
class QuarkusMdcUtilsTest {

  public static final QuarkusMdcData MDC_DATA =
      new QuarkusMdcData("user", "userId", "companyId", "msgId", "[LOG-1] ");

  @BeforeEach
  void setUp() {
    MDC.clear();
  }

  @Test
  void testExecuteWithNewMdcData(final OutputCapture output) {
    log.info("Logging with no MDC data");

    assertOutputNoMdcData(output);

    executeWithNewMdcData(
        () -> {
          assertNewMdcData();

          log.info("Logging with MDC data");
        });

    assertMdcDataWasCleared();
  }

  @SneakyThrows
  @Test
  void testExecuteWithNewMdcDataWithException(final OutputCapture output) {
    log.info("Logging with no MDC data");

    assertOutputNoMdcData(output);

    assertThrows(
        IOException.class,
        () ->
            executeWithNewMdcDataWithException(
                () -> {
                  assertNewMdcData();

                  log.info("Logging with MDC data");

                  testThrows();
                }));

    executeWithNewMdcDataWithException(
        () -> {
          assertNewMdcData();

          log.info("Logging with MDC data");
        });

    assertMdcDataWasCleared();
  }

  @Test
  void testExecuteWithNewMdcDataWithReturn(final OutputCapture output) {
    log.info("Logging with no MDC data");

    assertOutputNoMdcData(output);

    final int res =
        executeWithNewMdcData(
            () -> {
              assertNewMdcData();

              log.info("Logging with MDC data");

              return 1;
            });

    assertThat(res).isEqualTo(1);

    assertMdcDataWasCleared();
  }

  @SneakyThrows
  @SuppressWarnings("ConstantValue")
  @Test
  void testExecuteWithNewMdcDataWithExceptionWithReturn(final OutputCapture output) {
    log.info("Logging with no MDC data");

    assertOutputNoMdcData(output);

    final boolean doThrow = true;

    assertThrows(
        IOException.class,
        () ->
            executeWithNewMdcDataWithException(
                () -> {
                  assertNewMdcData();

                  log.info("Logging with MDC data");

                  if (doThrow) {
                    testThrows();
                  }

                  return 1;
                }));

    final Integer res =
        executeWithNewMdcDataWithException(
            () -> {
              assertNewMdcData();

              log.info("Logging with MDC data");

              return 1;
            });

    assertThat(res).isEqualTo(1);

    assertMdcDataWasCleared();
  }

  @Test
  void testExecuteWithMdcData(final OutputCapture output) {
    log.info("Logging with no MDC data");

    assertOutputNoMdcData(output);

    executeWithMdcData(
        MDC_DATA,
        () -> {
          assertMdcData();

          log.info("Logging with MDC data");

          assertOutputSetMdcData(output);
        });

    assertMdcDataWasCleared();

    assertThat(output.toString()).contains("[LOG-1] Logging with MDC data");
  }

  @Test
  void testExecuteWithMdcDataWithoutClear(final OutputCapture output) {
    log.info("Logging with no MDC data");

    assertOutputNoMdcData(output);

    MDC.put("key1", "value1");

    executeWithMdcDataWithoutClear(
        MDC_DATA,
        () -> {
          assertMdcData();

          log.info("Logging with MDC data");

          assertOutputSetMdcData(output);
        });

    assertThat(MDC.get("key1")).isEqualTo("value1");
  }

  @SneakyThrows
  @Test
  void testExecuteWithMdcDataWithException(final OutputCapture output) {
    log.info("Logging with no MDC data");

    assertOutputNoMdcData(output);

    assertThrows(
        IOException.class,
        () ->
            executeWithMdcDataWithException(
                MDC_DATA,
                () -> {
                  assertMdcData();

                  log.info("Logging with MDC data");

                  assertOutputSetMdcData(output);

                  testThrows();
                }));

    executeWithMdcDataWithException(
        MDC_DATA,
        () -> {
          assertMdcData();

          log.info("Logging with MDC data");

          assertOutputSetMdcData(output);
        });

    assertMdcDataWasCleared();
  }

  @SneakyThrows
  @Test
  void testExecuteWithMdcDataWithoutClearWithException(final OutputCapture output) {
    log.info("Logging with no MDC data");

    assertOutputNoMdcData(output);

    MDC.put("key1", "value1");

    assertThrows(
        IOException.class,
        () ->
            executeWithMdcDataWithException(
                MDC_DATA,
                () -> {
                  assertMdcData();

                  log.info("Logging with MDC data");

                  assertOutputSetMdcData(output);

                  testThrows();
                }));

    assertThat(MDC.get("key1")).isNull();

    MDC.put("key1", "value1");

    executeWithMdcDataWithoutClearWithException(
        MDC_DATA,
        () -> {
          assertMdcData();

          log.info("Logging with MDC data");

          assertOutputSetMdcData(output);
        });

    assertThat(MDC.get("key1")).isEqualTo("value1");
  }

  @Test
  void testExecuteWithMdcDataReturn(final OutputCapture output) {
    log.info("Logging with no MDC data");

    assertOutputNoMdcData(output);

    final Long res =
        executeWithMdcData(
            MDC_DATA,
            () -> {
              assertMdcData();

              log.info("Logging with MDC data");

              assertOutputSetMdcData(output);

              return null;
            });

    assertThat(res).isNull();

    assertMdcDataWasCleared();
  }

  @Test
  void testExecuteWithMdcDataWithoutClearReturn(final OutputCapture output) {
    log.info("Logging with no MDC data");

    assertOutputNoMdcData(output);

    setMdcKey("key1", "value1");

    final Long res =
        executeWithMdcDataWithoutClear(
            MDC_DATA,
            () -> {
              assertMdcData();

              log.info("Logging with MDC data");

              assertOutputSetMdcData(output);

              return null;
            });

    assertThat(res).isNull();

    assertThat(MDC.get("key1")).isEqualTo("value1");
  }

  @SneakyThrows
  @SuppressWarnings("ConstantValue")
  @Test
  void testExecuteWithMdcDataWithExceptionReturn(final OutputCapture output) {
    log.info("Logging with no MDC data");

    assertOutputNoMdcData(output);

    final boolean doThrow = true;

    assertThrows(
        IOException.class,
        () ->
            executeWithMdcDataWithException(
                MDC_DATA,
                () -> {
                  assertMdcData();

                  log.info("Logging with MDC data");

                  assertOutputSetMdcData(output);

                  if (doThrow) {
                    testThrows();
                  }

                  return null;
                }));

    final String res =
        executeWithMdcDataWithException(
            MDC_DATA,
            () -> {
              assertMdcData();

              log.info("Logging with MDC data");

              assertOutputSetMdcData(output);

              return "test";
            });

    assertThat(res).isEqualTo("test");

    assertMdcDataWasCleared();
  }

  @SneakyThrows
  @SuppressWarnings("ConstantValue")
  @Test
  void testExecuteWithMdcDataWithoutClearWithExceptionReturn(final OutputCapture output) {
    log.info("Logging with no MDC data");

    assertOutputNoMdcData(output);

    final boolean doThrow = true;

    MDC.put("key1", "value1");

    assertThrows(
        IOException.class,
        () ->
            executeWithMdcDataWithException(
                MDC_DATA,
                () -> {
                  assertMdcData();

                  log.info("Logging with MDC data");

                  assertOutputSetMdcData(output);

                  if (doThrow) {
                    testThrows();
                  }

                  return null;
                }));

    assertThat(MDC.get("key1")).isNull();

    MDC.put("key1", "value1");

    final String res =
        executeWithMdcDataWithoutClearWithException(
            MDC_DATA,
            () -> {
              assertMdcData();

              log.info("Logging with MDC data");

              assertOutputSetMdcData(output);

              return "test";
            });

    assertThat(res).isEqualTo("test");

    assertThat(MDC.get("key1")).isEqualTo("value1");
  }

  @Test
  void testGetCurrentMdcData() {
    final QuarkusMdcData mdcData = MDC_DATA;

    executeWithMdcData(
        mdcData,
        () -> {
          assertThat(getCurrentMdcData()).isEqualTo(mdcData);
        });
  }

  @Test
  void setMdcDataWithSomeMissingValues() {
    final var mdcData = new QuarkusMdcData("user", "userId", "companyId", "msgId", "[LOG-PREFIX]");

    final var originalAdapter = MDC.getMDCAdapter();
    setMdcAdapter(new ValueRequiredMDCAdapter(originalAdapter));

    try {
      executeWithMdcData(
          mdcData,
          () -> {
            assertThat(getCurrentMdcData()).isEqualTo(mdcData);
          });
    } finally {
      setMdcAdapter(originalAdapter);
    }
  }

  private void testThrows() throws IOException {
    throw new IOException("Test exception");
  }

  private static void assertOutputNoMdcData(final OutputCapture output) {
    assertThat(output.toString()).contains("MDC:user=,userId=,companyId=,msgId=");
  }

  private static void assertOutputSetMdcData(final OutputCapture output) {
    assertThat(output.toString())
        .contains("MDC:user=user,userId=userId,companyId=companyId,msgId=msgId");
  }

  private static void assertNewMdcData() {
    assertThat(MDC.get(MDC_USER)).isNullOrEmpty();
    assertThat(MDC.get(MDC_USER_ID)).isNullOrEmpty();
    assertThat(MDC.get(MDC_COMPANY_ID)).isNullOrEmpty();
    assertThat(MDC.get(MDC_MSG_ID)).isNullOrEmpty();
    assertThat(MDC.get(MDC_LOG_PREFIX)).isNullOrEmpty();
  }

  private static void assertMdcData() {
    assertThat(MDC.get(MDC_USER)).isEqualTo("user");
    assertThat(MDC.get(MDC_USER_ID)).isEqualTo("userId");
    assertThat(MDC.get(MDC_COMPANY_ID)).isEqualTo("companyId");
    assertThat(MDC.get(MDC_MSG_ID)).isEqualTo("msgId");
    assertThat(MDC.get(MDC_LOG_PREFIX)).isEqualTo("[LOG-1] ");
  }

  private void assertMdcDataWasCleared() {
    assertThat(MDC.get(MDC_USER)).isNull();
    assertThat(MDC.get(MDC_USER_ID)).isNull();
    assertThat(MDC.get(MDC_COMPANY_ID)).isNull();
    assertThat(MDC.get(MDC_MSG_ID)).isNull();
    assertThat(MDC.get(MDC_LOG_PREFIX)).isNull();
  }

  private void setMdcAdapter(final MDCAdapter adapter) {
    try {

      final Field adapterField = MDC.class.getDeclaredField("MDC_ADAPTER");
      adapterField.setAccessible(true);
      adapterField.set(null, adapter);
    } catch (final NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
      throw new RuntimeException("Can't set MDC adapter via reflection", e);
    }
  }

  /** An implementation of MDCAdapter which does not accept <code>null</code>-s for values. */
  private static class ValueRequiredMDCAdapter implements MDCAdapter {

    private final MDCAdapter delegate;

    public ValueRequiredMDCAdapter(final MDCAdapter delegate) {
      this.delegate = delegate;
    }

    @Override
    public void put(final String key, final String val) {
      Objects.requireNonNull(val);
      delegate.put(key, val);
    }

    @Override
    public String get(final String key) {
      return delegate.get(key);
    }

    @Override
    public void remove(final String key) {
      delegate.remove(key);
    }

    @Override
    public void clear() {
      delegate.clear();
    }

    @Override
    public Map<String, String> getCopyOfContextMap() {
      return delegate.getCopyOfContextMap();
    }

    @Override
    public void setContextMap(final Map<String, String> contextMap) {
      delegate.setContextMap(contextMap);
    }

    @Override
    public void pushByKey(final String key, final String value) {}

    @Override
    public String popByKey(final String key) {
      return "";
    }

    @Override
    public Deque<String> getCopyOfDequeByKey(final String key) {
      return null;
    }

    @Override
    public void clearDequeByKey(final String key) {}
  }
}
