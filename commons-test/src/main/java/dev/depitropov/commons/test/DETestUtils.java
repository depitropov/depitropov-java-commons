package dev.depitropov.commons.test;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.exception.ExceptionUtils.getThrowableList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

public final class DETestUtils {

  static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @SneakyThrows
  public static String toJson(final Object obj) {
    return objectMapper.writeValueAsString(obj);
  }

  @SneakyThrows
  public static void assertJson(final String expectedJson, final Object value) {
    final String actualJson =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);

    assertEquals(toFormattedJson(expectedJson), actualJson);
  }

  @SneakyThrows
  public static void assertJson(final String expectedJson, final String actualJson) {
    assertEquals(toFormattedJson(expectedJson), toFormattedJson(actualJson));
  }

  @SneakyThrows
  public static String toFormattedJson(final String json) {
    return objectMapper
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(objectMapper.readTree(json));
  }

  @SneakyThrows
  public static Instant getNowAfter1ms() {
    Thread.sleep(1);
    final Instant logTime = Instant.now();
    Thread.sleep(1);

    return logTime;
  }

  public static <T extends Throwable> void assertThrowsMessage(
      final Class<T> thClass,
      final Executable executable,
      final String errorMessage,
      final Object... args) {
    final T exception = assertThrows(thClass, executable);

    if (isNotEmpty(errorMessage)) {
      final String theMassageValue = String.format(errorMessage, args);
      assertEquals(theMassageValue, exception.getMessage());
    }
  }

  public static Throwable assertThrowableCauseContains(
      final Throwable throwable, final Class<?> expectedCause) {

    final List<Throwable> throwables = getThrowableList(throwable);

    for (final Throwable iter : throwables) {
      if (iter.getClass().equals(expectedCause)) {
        return iter;
      }
    }

    throw new AssertionFailedError("Unexpected cause: " + expectedCause + " in " + throwable);
  }
}
