package dev.depitropov.commons.quarkus;

import static dev.depitropov.commons.DEStringUtils.LOG_MAX_DATA_LENGTH;
import static dev.depitropov.commons.DEStringUtils.unzipBase64Data;
import static dev.depitropov.commons.quarkus.LogData.LOG_DATA_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import com.fasterxml.jackson.databind.type.SimpleType;
import dev.depitropov.commons.ObjectSerializer;
import dev.depitropov.commons.impl.ObjectSerializerImpl;
import dev.depitropov.commons.quarkus.loggingjson.CustomJacksonJsonFactory;
import io.quarkiverse.loggingjson.JsonFactory;
import io.quarkiverse.loggingjson.JsonGenerator;
import io.quarkiverse.loggingjson.StringBuilderWriter;
import io.quarkiverse.loggingjson.jackson.JacksonJsonFactory;
import io.quarkiverse.loggingjson.providers.StructuredArgument;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogDataTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ObjectSerializer objectSerializer;
  private LogData logData;

  @BeforeEach
  void setUp() {
    objectSerializer = new ObjectSerializerImpl(new ObjectMapper());
    logData = new LogData(objectSerializer);
  }

  @Test
  void smallPayloadIsEmittedAsJsonObjectNotEscapedString() throws IOException {
    final SmallPayload payload = new SmallPayload("abc", 7);

    for (final JsonFactory factory : jsonFactories()) {
      final JsonNode data = writeData(factory, logData.getKv(payload)).get(LOG_DATA_KEY);

      assertThat(data.isObject()).isTrue();
      assertThat(data.get("id").asText()).isEqualTo("abc");
      assertThat(data.get("n").asInt()).isEqualTo(7);
    }
  }

  @Test
  void largePayloadIsEmittedAsGzipBase64String() throws IOException {
    final SmallPayload payload = new SmallPayload("x".repeat(LOG_MAX_DATA_LENGTH), 1);
    final String expectedJson = objectSerializer.toStringSuppressLog(payload);

    assertThat(expectedJson.length()).isGreaterThan(LOG_MAX_DATA_LENGTH);

    for (final JsonFactory factory : jsonFactories()) {
      final JsonNode data = writeData(factory, logData.getKv(payload)).get(LOG_DATA_KEY);

      assertThat(data.isTextual()).isTrue();
      assertThat(unzipBase64Data(data.asText())).isEqualTo(expectedJson);
    }
  }

  @Test
  void nullPayloadIsEmittedAsEmptyString() throws IOException {
    for (final JsonFactory factory : jsonFactories()) {
      final JsonNode data = writeData(factory, logData.getKv(null)).get(LOG_DATA_KEY);

      assertThat(data.isTextual()).isTrue();
      assertThat(data.asText()).isEmpty();
    }
  }

  @Test
  void emptyPayloadIsEmittedAsEmptyString() throws IOException {
    for (final JsonFactory factory : jsonFactories()) {
      final JsonNode data = writeData(factory, logData.getKv("")).get(LOG_DATA_KEY);

      assertThat(data.isTextual()).isTrue();
      assertThat(data.asText()).isEmpty();
    }
  }

  @Test
  void stringPayloadUnderThresholdIsEmittedAsJsonString() throws IOException {
    for (final JsonFactory factory : jsonFactories()) {
      final JsonNode data = writeData(factory, logData.getKv("hello")).get(LOG_DATA_KEY);

      assertThat(data.isTextual()).isTrue();
      assertThat(data.asText()).isEqualTo("hello");
    }
  }

  @Test
  void failedSerializationIsEmittedAsEmptyString() throws IOException {
    for (final JsonFactory factory : jsonFactories()) {
      final JsonNode data =
          writeData(factory, logData.getKv(new UnserializablePayload())).get(LOG_DATA_KEY);

      assertThat(data.isTextual()).isTrue();
      assertThat(data.asText()).isEmpty();
    }
  }

  @Test
  void payloadIsSerializedOnceWhenWrittenToJson() throws IOException {
    final CountedPayload payload = new CountedPayload();

    final StructuredArgument argument = logData.getKv(payload);
    final int afterGetKv = payload.serializeCount();

    assertThat(afterGetKv).isEqualTo(1);

    for (final JsonFactory factory : jsonFactories()) {
      writeData(factory, argument);
    }

    assertThat(payload.serializeCount()).isEqualTo(afterGetKv);
  }

  private static List<JsonFactory> jsonFactories() {
    return List.of(new JacksonJsonFactory(), new CustomJacksonJsonFactory());
  }

  private static JsonNode writeData(final JsonFactory factory, final StructuredArgument argument)
      throws IOException {
    final StringBuilderWriter writer = new StringBuilderWriter();

    try (JsonGenerator generator = factory.createGenerator(writer)) {
      generator.writeStartObject();
      argument.writeTo(generator);
      generator.writeEndObject();
    }

    return MAPPER.readTree(writer.toString());
  }

  record SmallPayload(String id, int n) {}

  static final class CountedPayload {

    private final AtomicInteger serializeCount = new AtomicInteger();

    public String getId() {
      serializeCount.incrementAndGet();

      return "x";
    }

    int serializeCount() {
      return serializeCount.get();
    }
  }

  static final class UnserializablePayload {

    @SuppressWarnings("unused")
    public int getId() throws JacksonException {
      throw InvalidNullException.from(null, SimpleType.constructUnsafe(String.class), "Test error");
    }
  }
}
