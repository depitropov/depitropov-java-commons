package dev.depitropov.commons;

import static dev.depitropov.commons.DEFileUtils.safeDeleteDir;

import java.nio.file.Files;
import java.nio.file.Path;

public final class TempDirectoryUtils {

  private static final String TEMP_PATH_PREFIX = "temp-dir-";

  static class TempDirectoryUtilsException extends RuntimeException {

    public TempDirectoryUtilsException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  @SuppressWarnings("java:S5443")
  public static <T> T executeWithTempDirectory(final CheckedFunction<Path, T> function) {
    Path tempDirectory = null;

    try {
      tempDirectory = Files.createTempDirectory(TEMP_PATH_PREFIX);

      tempDirectory.toFile().deleteOnExit();

      return function.apply(tempDirectory);

    } catch (final Exception exception) {
      throw new TempDirectoryUtilsException("Execution in temp directory failed", exception);
    } finally {
      safeDeleteDir(tempDirectory, TEMP_PATH_PREFIX);
    }
  }

  public static void executeWithTempDirectory(final CheckedConsumer<Path> consumer) {
    executeWithTempDirectory(
        (CheckedFunction<Path, Void>)
            path -> {
              consumer.accept(path);

              return null;
            });
  }

  @SuppressWarnings("java:S112")
  @FunctionalInterface
  public interface CheckedFunction<T, R> {

    R apply(T t) throws Exception;
  }

  @SuppressWarnings("java:S112")
  @FunctionalInterface
  public interface CheckedConsumer<T> {

    void accept(T t) throws Exception;
  }
}
