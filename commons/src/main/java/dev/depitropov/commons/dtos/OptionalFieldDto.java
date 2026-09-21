package dev.depitropov.commons.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OptionalFieldDto<T> {

  private final T value;

  @JsonCreator
  public OptionalFieldDto(@JsonProperty("value") final T value) {

    this.value = value;
  }

  public static <T> OptionalFieldDto<T> empty() {

    return new OptionalFieldDto<>(null);
  }

  public static <T> OptionalFieldDto<T> of(final T value) {

    return new OptionalFieldDto<>(value);
  }

  @JsonIgnore
  public boolean isEmpty() {

    return value == null;
  }
}
