package dev.depitropov.commons;

import static java.util.Objects.isNull;

import java.util.function.Function;

public final class DEObjectUtils {

  /** Applies {@code extractor} unless {@code object} is null, in which case returns null. */
  public static <T, V> T safeGet(final V object, final Function<V, T> extractor) {
    return safeGet(object, extractor, null);
  }

  /** Applies {@code extractor} unless {@code object} is null, in which case returns the default. */
  public static <T, V> T safeGet(
      final V object, final Function<V, T> extractor, final T defaultValue) {
    if (isNull(object)) {
      return defaultValue;
    }

    return extractor.apply(object);
  }

  public static <T> T getDefaultIfNull(final T value, final T defaultValue) {
    return value == null ? defaultValue : value;
  }
}
