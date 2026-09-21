package dev.depitropov.commons;

import static dev.depitropov.commons.DEUuidUtils.*;
import static dev.depitropov.commons.MdcKeys.MDC_COMPANY_ID;
import static dev.depitropov.commons.MdcKeys.MDC_LOG_PREFIX;
import static dev.depitropov.commons.MdcKeys.MDC_MSG_ID;
import static dev.depitropov.commons.MdcKeys.MDC_SPAN_ID;
import static dev.depitropov.commons.MdcKeys.MDC_TRACE_ID;
import static dev.depitropov.commons.MdcKeys.MDC_USER;
import static dev.depitropov.commons.MdcKeys.MDC_USER_AGENT;
import static dev.depitropov.commons.MdcKeys.MDC_USER_ID;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.function.Supplier;
import org.slf4j.MDC;

@SuppressWarnings("java:S112")
public final class MdcUtils {

  public interface SupplierWithException<T> {
    T get() throws Exception;
  }

  public interface RunnableWithException {
    void run() throws Exception;
  }

  public static void executeWithNewMdcData(final Runnable runnable) {
    executeWithNewMdcData(
        () -> {
          runnable.run();
          return null;
        });
  }

  public static <T> T executeWithNewMdcData(final Supplier<T> supplier) {
    try {
      setNewMdcData();

      return supplier.get();
    } finally {
      MDC.clear();
    }
  }

  public static void executeWithNewMdcDataWithException(final RunnableWithException runnable)
      throws Exception {
    executeWithNewMdcDataWithException(
        () -> {
          runnable.run();
          return null;
        });
  }

  public static <T> T executeWithNewMdcDataWithException(final SupplierWithException<T> supplier)
      throws Exception {
    try {
      setNewMdcData();

      return supplier.get();
    } finally {
      MDC.clear();
    }
  }

  public static void executeWithMdcData(final MdcData mdcData, final Runnable runnable) {
    executeWithMdcData(
        mdcData,
        () -> {
          runnable.run();
          return null;
        });
  }

  public static void executeWithMdcDataWithoutClear(
      final MdcData mdcData, final Runnable runnable) {
    executeWithMdcDataWithoutClear(
        mdcData,
        () -> {
          runnable.run();
          return null;
        });
  }

  public static <T> T executeWithMdcData(final MdcData mdcData, final Supplier<T> supplier) {
    try {
      return executeWithMdcDataWithoutClear(mdcData, supplier);
    } finally {
      MDC.clear();
    }
  }

  public static <T> T executeWithMdcDataWithoutClear(
      final MdcData mdcData, final Supplier<T> supplier) {

    final MdcData currentMdcData = getCurrentMdcData();

    setMdcData(mdcData);

    try {
      return supplier.get();
    } finally {
      setMdcData(currentMdcData);
    }
  }

  public static void executeWithMdcDataWithException(
      final MdcData mdcData, final RunnableWithException runnable) throws Exception {
    executeWithMdcDataWithException(
        mdcData,
        () -> {
          runnable.run();
          return null;
        });
  }

  public static void executeWithMdcDataWithoutClearWithException(
      final MdcData mdcData, final RunnableWithException runnable) throws Exception {
    executeWithMdcDataWithoutClearWithException(
        mdcData,
        () -> {
          runnable.run();
          return null;
        });
  }

  public static <T> T executeWithMdcDataWithException(
      final MdcData mdcData, final SupplierWithException<T> supplier) throws Exception {
    try {
      return executeWithMdcDataWithoutClearWithException(mdcData, supplier);
    } finally {
      MDC.clear();
    }
  }

  public static <T> T executeWithMdcDataWithoutClearWithException(
      final MdcData mdcData, final SupplierWithException<T> supplier) throws Exception {

    final MdcData currentMdcData = getCurrentMdcData();

    setMdcData(mdcData);

    try {
      return supplier.get();
    } finally {
      setMdcData(currentMdcData);
    }
  }

  public static void setMdcKey(final String key, final String value) {
    if (trimToNull(value) != null) {
      MDC.put(key, value);
    } else {
      MDC.remove(key);
    }
  }

  private static void setNewMdcData() {
    MDC.clear();
  }

  private static void setMdcData(final MdcData mdcData) {

    setMdcKey(MDC_TRACE_ID, mdcData.traceId());
    setMdcKey(MDC_SPAN_ID, mdcData.spanId());

    setMdcKey(MDC_USER, mdcData.user());
    setMdcKey(MDC_USER_ID, mdcData.userId());
    setMdcKey(MDC_COMPANY_ID, mdcData.companyId());

    setMdcKey(MDC_MSG_ID, mdcData.msgId());

    setMdcKey(MDC_LOG_PREFIX, mdcData.logPrefix());

    setMdcKey(MDC_USER_AGENT, mdcData.userAgent());
  }

  public static MdcData getCurrentMdcData() {
    return new MdcData(
        MDC.get(MDC_TRACE_ID),
        MDC.get(MDC_SPAN_ID),
        MDC.get(MDC_USER),
        MDC.get(MDC_USER_ID),
        MDC.get(MDC_COMPANY_ID),
        MDC.get(MDC_MSG_ID),
        MDC.get(MDC_LOG_PREFIX),
        MDC.get(MDC_USER_AGENT));
  }
}
