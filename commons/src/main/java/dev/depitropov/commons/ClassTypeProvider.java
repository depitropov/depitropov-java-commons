package dev.depitropov.commons;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import lombok.Value;

@Value
public class ClassTypeProvider<T> {

  public static <T> ClassTypeProvider<T> of(final Class<T> tClass) {
    return new ClassTypeProvider<>(tClass);
  }

  public static <T> ClassTypeProvider<T> of(final TypeReference<T> typeReference) {
    return new ClassTypeProvider<>(typeReference);
  }

  public static <T> ClassTypeProvider<T> of(final JavaType javaType) {
    return new ClassTypeProvider<>(javaType);
  }

  private ClassTypeProvider(final Class<T> tClass) {

    requireNonNull(tClass);

    this.tClass = tClass;
    typeReference = null;
    javaType = null;
  }

  private ClassTypeProvider(final TypeReference<T> typeReference) {

    requireNonNull(typeReference);

    tClass = null;
    this.typeReference = typeReference;
    javaType = null;
  }

  private ClassTypeProvider(final JavaType javaType) {

    requireNonNull(javaType);

    tClass = null;
    typeReference = null;
    this.javaType = javaType;
  }

  Class<T> tClass;

  TypeReference<T> typeReference;

  JavaType javaType;
}
