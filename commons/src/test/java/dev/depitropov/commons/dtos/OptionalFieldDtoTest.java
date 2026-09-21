package dev.depitropov.commons.dtos;

import static dev.depitropov.commons.DEResourceUtils.loadResource;
import static dev.depitropov.commons.DETestUtils.assertJson;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

public class OptionalFieldDtoTest {

  @Test
  @SneakyThrows
  void optionalFieldDto_whenCreatedAsEmpty_should_ReturnEmptyTrue() {

    final OptionalFieldDto<String> optionalField = OptionalFieldDto.empty();

    assertTrue(optionalField.isEmpty());
    assertNull(optionalField.getValue());
  }

  @Test
  @SneakyThrows
  void optionalFieldDto_whenCreatedAsWithValue_should_ReturnEmptyFalse() {

    final OptionalFieldDto<String> optionalField = OptionalFieldDto.of("Hello world!");

    assertFalse(optionalField.isEmpty());
    assertThat(optionalField.getValue()).isEqualTo("Hello world!");
  }

  @Test
  @SneakyThrows
  void optionalFieldDto_should_beDeserializedFromJson() {

    final ObjectMapper mapper = new ObjectMapper();

    final String optionalFieldJson = loadResource("optional-field-value.json");

    final OptionalFieldDto<String> optionalFieldDto =
        mapper.readValue(optionalFieldJson, OptionalFieldDto.class);

    assertFalse(optionalFieldDto.isEmpty());
    assertThat(optionalFieldDto.getValue()).isEqualTo("Hello World!");
  }

  @Test
  @SneakyThrows
  void optionalFieldDto_withoutEmptyAndNullValue_should_beDeserializedFromJson() {

    final ObjectMapper mapper = new ObjectMapper();

    final String optionalFieldJson = loadResource("optional-field-empty.json");

    final OptionalFieldDto<String> optionalFieldDto =
        mapper.readValue(optionalFieldJson, OptionalFieldDto.class);

    assertTrue(optionalFieldDto.isEmpty());
    assertNull(optionalFieldDto.getValue());
  }

  @Test
  @SneakyThrows
  void optionalFieldDto_should_beSerializedToJson() {

    final ObjectMapper mapper = new ObjectMapper();

    final String optionalFieldJson = loadResource("optional-field-value.json");

    final OptionalFieldDto<String> optionalField = OptionalFieldDto.of("Hello World!");

    final String optionalFieldSerialized = mapper.writeValueAsString(optionalField);

    assertJson(optionalFieldJson, optionalFieldSerialized);
  }
}
