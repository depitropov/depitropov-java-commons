package dev.depitropov.commons.impl;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.Validate.notEmpty;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.depitropov.commons.ClassTypeProvider;
import dev.depitropov.commons.ObjectSerializer;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings({"java:S112", "unchecked"})
@Slf4j
public record ObjectSerializerImpl(ObjectMapper objectMapper) implements ObjectSerializer {

  @Override
  public String toString(final Object object) {

    if (object == null) {
      return "";
    }

    if (object instanceof final CharSequence string) {
      return string.toString();
    }

    String json = null;
    try {
      json = objectMapper.writeValueAsString(object);
    } catch (final JacksonException e) {
      log.error("Failed to write object to json, error: {}", e.getMessage(), e);
    }

    return json;
  }

  @Override
  public String toStringSuppressLog(final Object object) {

    if (object == null) {
      return "";
    }

    if (object instanceof final CharSequence string) {
      return string.toString();
    }

    String json = null;
    try {
      json = objectMapper.writeValueAsString(object);
    } catch (final JacksonException e) {
      // suppress
    }

    return json;
  }

  @Override
  public String toStringThrow(final Object object) {

    if (object == null) {
      return "";
    }

    if (object instanceof final CharSequence string) {
      return string.toString();
    }

    final String json;
    try {
      json = objectMapper.writeValueAsString(object);
    } catch (final JacksonException e) {
      log.error("Failed to write object to json, error: {}", e.getMessage(), e);

      throw new RuntimeException(e);
    }

    return json;
  }

  @Override
  public <T> T toObject(final String json, final ClassTypeProvider<T> classTypeProvider) {

    T object = null;

    try {
      object = toObjectInternal(json, classTypeProvider);
    } catch (final JacksonException e) {
      log.error("Failed to read json, error: {}, json: {}", e.getMessage(), json, e);
    }

    return object;
  }

  @Override
  public <T> T toObjectSuppressLog(
      final String json, final ClassTypeProvider<T> classTypeProvider) {

    T object = null;

    try {
      object = toObjectInternal(json, classTypeProvider);
    } catch (final JacksonException e) {
      // suppress
    }

    return object;
  }

  @Override
  public <T> T toObjectThrow(final String json, final ClassTypeProvider<T> classTypeProvider) {

    final T object;

    try {
      object = toObjectInternal(json, classTypeProvider);
    } catch (final JacksonException e) {
      log.error("Failed to read json, error: {}, json: {}", e.getMessage(), json, e);

      throw new RuntimeException(e);
    }

    return object;
  }

  private <T> T toObjectInternal(final String json, final ClassTypeProvider<T> classTypeProvider)
      throws JacksonException {

    notEmpty(json);
    requireNonNull(classTypeProvider);

    final T object;
    if (classTypeProvider.getTClass() != null) {
      if (classTypeProvider.getTClass().equals(String.class)) {
        return (T) json;
      }

      object = objectMapper.readValue(json, classTypeProvider.getTClass());
    } else if (classTypeProvider.getTypeReference() != null) {
      if (classTypeProvider
          .getTypeReference()
          .getType()
          .getTypeName()
          .equals(String.class.getTypeName())) {
        return (T) json;
      }

      object = objectMapper.readValue(json, classTypeProvider.getTypeReference());
    } else if (classTypeProvider.getJavaType() != null) {
      if (classTypeProvider.getJavaType().getRawClass().equals(String.class)) {
        return (T) json;
      }

      object = objectMapper.readValue(json, classTypeProvider.getJavaType());
    } else {
      throw new IllegalArgumentException("Missing class type");
    }

    return object;
  }
}
