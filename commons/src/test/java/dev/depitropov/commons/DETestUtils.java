package dev.depitropov.commons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.SneakyThrows;

public class DETestUtils {

  protected static ObjectMapper objectMapper = new ObjectMapper();

  @SneakyThrows
  public static void assertJson(final String expectedJson, final Object value) {
    final String actualJson =
        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);

    assertEquals(toFormattedJson(expectedJson), actualJson);
  }

  @SneakyThrows
  public static void assertJson(final String expectedJson, final String actualJson) {
    assertThat(toFormattedJson(actualJson)).isEqualTo(toFormattedJson(expectedJson));
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
}
