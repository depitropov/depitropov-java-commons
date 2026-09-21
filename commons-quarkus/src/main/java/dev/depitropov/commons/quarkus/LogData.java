package dev.depitropov.commons.quarkus;

import static dev.depitropov.commons.DEStringUtils.LOG_MAX_DATA_LENGTH;
import static dev.depitropov.commons.DEStringUtils.zipDataToBase64;
import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import com.fasterxml.jackson.databind.util.RawValue;
import dev.depitropov.commons.ObjectSerializer;
import io.quarkiverse.loggingjson.JsonGenerator;
import io.quarkiverse.loggingjson.providers.StructuredArgument;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;

@RequiredArgsConstructor
public class LogData {

  public static final String LOG_DATA_KEY = "data";

  private final ObjectSerializer objectSerializer;

  public void error(
      final Logger log,
      final Object data,
      final Throwable err,
      final String message,
      final Object... args) {

    if (log.isErrorEnabled()) {
      log.error(message, addAll(args, getKv(data), err));
    }
  }

  public void error(
      final Logger log, final Object data, final String message, final Object... args) {

    if (log.isErrorEnabled()) {
      log.error(message, addAll(args, getKv(data)));
    }
  }

  public void warn(
      final Logger log,
      final Object data,
      final Throwable err,
      final String message,
      final Object... args) {

    if (log.isWarnEnabled()) {
      log.warn(message, addAll(args, getKv(data), err));
    }
  }

  public void warn(
      final Logger log, final Object data, final String message, final Object... args) {

    if (log.isWarnEnabled()) {
      log.warn(message, addAll(args, getKv(data)));
    }
  }

  public void info(
      final Logger log, final Object data, final String message, final Object... args) {

    if (log.isInfoEnabled()) {
      log.info(message, addAll(args, getKv(data)));
    }
  }

  public void debug(
      final Logger log, final Object data, final String message, final Object... args) {

    if (log.isDebugEnabled()) {
      log.debug(message, addAll(args, getKv(data)));
    }
  }

  public void trace(
      final Logger log, final Object data, final String message, final Object... args) {

    if (log.isTraceEnabled()) {
      log.trace(message, addAll(args, getKv(data)));
    }
  }

  StructuredArgument getKv(final Object data) {
    return new LogDataStructuredArgument(objectSerializer.toStringSuppressLog(data), data);
  }

  /**
   * Emits the JSON that {@link ObjectSerializer#toStringSuppressLog} produced for the gzip
   * threshold.
   *
   * <p>quarkus-logging-json {@link JsonGenerator} has no {@code writeRawValue}. Jackson writes
   * {@link RawValue} through the generator codec as raw JSON, so the formatter does not serialize
   * the payload again.
   */
  private static final class LogDataStructuredArgument implements StructuredArgument {

    private final String value;
    private final boolean rawJson;

    private LogDataStructuredArgument(final String serialized, final Object data) {
      final String json = trimToEmpty(serialized);

      if (isEmpty(json) || data == null) {
        this.value = "";
        this.rawJson = false;
      } else if (json.length() > LOG_MAX_DATA_LENGTH) {
        this.value = zipDataToBase64(json);
        this.rawJson = false;
      } else if (data instanceof CharSequence) {
        this.value = data.toString();
        this.rawJson = false;
      } else {
        this.value = json;
        this.rawJson = true;
      }
    }

    @Override
    public void writeTo(final JsonGenerator generator) throws IOException {
      if (rawJson) {
        generator.writeFieldName(LOG_DATA_KEY);
        generator.writeObject(new RawValue(value));
      } else {
        generator.writeStringField(LOG_DATA_KEY, value);
      }
    }

    @Override
    public String toString() {
      return value;
    }
  }
}
