package dev.depitropov.commons;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.FileUtils.openOutputStream;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.Validate.notEmpty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

@Slf4j
public final class DEFileUtils {

  public static final int COPY_STREAMS_BUFFER_SIZE = 8192;

  static class DEFileUtilsException extends RuntimeException {

    public DEFileUtilsException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

  public static void safeDeleteDir(final Path dirToDel, final String checkPath) {
    if (dirToDel == null) {
      return;
    }

    notEmpty(checkPath);

    final File dirFile = dirToDel.toFile();

    if (dirFile.exists()
        && dirFile.isDirectory()
        && dirFile.getAbsolutePath().contains(checkPath)) {

      try {
        FileUtils.deleteDirectory(dirFile);
      } catch (final IOException e) {
        throw new DEFileUtilsException("Failed to delete dir: " + dirFile.getAbsolutePath(), e);
      }
    }
  }

  public static void deleteFile(final Path dataPath) {
    try {
      Files.deleteIfExists(dataPath);
    } catch (final Exception e) {
      log.error("Failed to delete file: {}", dataPath, e);
    }
  }

  public static String setLFLineSep(final String content) {
    if (isEmpty(content)) {
      return content;
    }

    String res = content.replace("\r\n", "\n");

    res = res.replace("\r", "\n");

    return res;
  }

  public static long copyURLToFile(
      final URL source,
      final File destination,
      final int connectionTimeoutMillis,
      final int readTimeoutMillis,
      final long maxAllowedSize)
      throws IOException {
    final URLConnection connection = source.openConnection();
    connection.setConnectTimeout(connectionTimeoutMillis);
    connection.setReadTimeout(readTimeoutMillis);

    final InputStream stream = connection.getInputStream();

    Exception workingException = null;

    try {
      return copyInputStreamToFile(stream, destination, maxAllowedSize);
    } catch (final Exception e) {
      workingException = e;
      throw e;
    } finally {
      if (stream != null) {
        if (workingException != null) {
          try {
            stream.close();
          } catch (final Exception var14) {
            workingException.addSuppressed(var14);
          }
        } else {
          stream.close();
        }
      }
    }
  }

  public static long copyInputStreamToFile(
      final InputStream inputStream, final File destination, final long maxAllowedSize)
      throws IOException {

    final OutputStream out = openOutputStream(destination);

    Exception workingException = null;

    try {
      return copyStreams(inputStream, out, COPY_STREAMS_BUFFER_SIZE, maxAllowedSize);
    } catch (final Exception e) {
      workingException = e;
      throw e;
    } finally {
      if (workingException != null) {
        try {
          out.close();
        } catch (final Exception var11) {
          workingException.addSuppressed(var11);
        }
      } else {
        out.close();
      }
    }
  }

  public static long copyStreams(
      final InputStream inputStream,
      final OutputStream outputStream,
      int bufferSize,
      final long maxAllowedSize)
      throws IOException {

    requireNonNull(inputStream, "inputStream");
    requireNonNull(outputStream, "outputStream");

    if (bufferSize <= 0) {
      bufferSize = COPY_STREAMS_BUFFER_SIZE;
    }

    final byte[] buffer = new byte[bufferSize];

    long count;
    int n;
    for (count = 0L; -1 != (n = inputStream.read(buffer)); count += n) {
      outputStream.write(buffer, 0, n);

      if (maxAllowedSize > 0 && count > maxAllowedSize) {
        throw new IOException("Max allowed size reached: " + maxAllowedSize);
      }
    }

    return count;
  }
}
