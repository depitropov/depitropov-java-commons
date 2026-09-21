package dev.depitropov.commons;

import static dev.depitropov.commons.DEFileUtils.setLFLineSep;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/** Loads a resource from the filesystem first, then from the classpath. */
public final class DEResourceUtils {

  public static final String FAILED_TO_LOAD_ERROR = "Failed to load resource: ";

  public static class DEResourceUtilsException extends RuntimeException {

    public DEResourceUtilsException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Resolves a resource as a stream, trying in order: the filesystem, this class, this class's
   * loader, the system loader, then the thread context loader.
   *
   * @param resource path or classpath resource name
   * @return an open stream the caller must close
   * @throws IOException if the resource is found nowhere
   */
  public static InputStream findResourceInputStream(final String resource) throws IOException {
    final Path path = Paths.get(resource);
    if (Files.exists(path) && !Files.isDirectory(path)) {
      return new FileInputStream(path.toFile());
    }

    InputStream is = DEResourceUtils.class.getResourceAsStream(resource);
    if (is != null) {
      return is;
    }

    is = DEResourceUtils.class.getClassLoader().getResourceAsStream(resource);
    if (is != null) {
      return is;
    }

    is = ClassLoader.getSystemResourceAsStream(resource);
    if (is != null) {
      return is;
    }

    is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    if (is != null) {
      return is;
    }

    throw new IOException("Failed to find: " + resource);
  }

  public static String loadResource(final String resource) {
    try (final InputStream is = findResourceInputStream(resource)) {
      return IOUtils.toString(is, UTF_8.name());
    } catch (final IOException e) {
      throw new DEResourceUtilsException(FAILED_TO_LOAD_ERROR + resource, e);
    }
  }

  public static String loadResourceLFLineSep(final String resource) {
    try (final InputStream is = findResourceInputStream(resource)) {
      return setLFLineSep(IOUtils.toString(is, UTF_8.name()));
    } catch (final IOException e) {
      throw new DEResourceUtilsException(FAILED_TO_LOAD_ERROR + resource, e);
    }
  }

  public static String loadResourceSystemLineSep(final String resource) {
    try (final InputStream is = findResourceInputStream(resource)) {
      try (final InputStreamReader streamReader =
              new InputStreamReader(is, StandardCharsets.UTF_8);
          final BufferedReader reader = new BufferedReader(streamReader)) {
        return reader.lines().collect(Collectors.joining(System.lineSeparator()));
      }
    } catch (final IOException e) {
      throw new DEResourceUtilsException(FAILED_TO_LOAD_ERROR + resource, e);
    }
  }

  public static byte[] loadResourceAsBytes(final String resource) {
    try (final InputStream is = findResourceInputStream(resource)) {
      return IOUtils.toByteArray(is);
    } catch (final IOException e) {
      throw new DEResourceUtilsException(FAILED_TO_LOAD_ERROR + resource, e);
    }
  }

  public static void copyToFile(final String resource, final Path srcPath) throws IOException {
    try (final InputStream is = findResourceInputStream(resource)) {
      FileUtils.copyInputStreamToFile(is, srcPath.toFile());
    }
  }
}
