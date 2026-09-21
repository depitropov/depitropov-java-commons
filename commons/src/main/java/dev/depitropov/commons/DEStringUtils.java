package dev.depitropov.commons;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public final class DEStringUtils {

  public static final int MAX_VALUE_CHARS_IN_TO_STRING = 128;

  /** Cloud logging backends commonly cap a log entry around 8K, so compress above this. */
  public static final int LOG_MAX_DATA_LENGTH = 6 * 1024;

  /** Letters, marks, numbers, punctuation, symbols, separators and tab. */
  public static final String PRINTABLE_CHARS = "\\p{L}\\p{M}\\p{N}\\p{P}\\p{S}\\p{Z}\t";

  /** {@link #PRINTABLE_CHARS} as a bean-validation character set, rejecting blank-only input. */
  public static final String PRINTABLE_CHARS_SET = "(?=.*\\S)[" + PRINTABLE_CHARS + "]";

  /** {@link #PRINTABLE_CHARS} plus line breaks. */
  public static final String PRINTABLE_CHARS_WITH_NEW_LINES =
      "\\p{L}\\p{M}\\p{N}\\p{P}\\p{S}\\p{Z}\\s";

  /** {@link #PRINTABLE_CHARS_WITH_NEW_LINES} as a bean-validation character set. */
  public static final String PRINTABLE_CHARS_WITH_NEW_LINES_SET =
      "(?=.*\\S)[" + PRINTABLE_CHARS_WITH_NEW_LINES + "]";

  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(?U)\\s+");
  private static final Pattern HORIZONTAL_WHITESPACE_PATTERN = Pattern.compile("(?U)[^\\S\\r\\n]+");
  private static final Pattern NON_PRINTABLE_PATTERN =
      Pattern.compile("[^" + PRINTABLE_CHARS + "]");
  private static final Pattern NON_PRINTABLE_WITH_NEW_LINES_PATTERN =
      Pattern.compile("[^" + PRINTABLE_CHARS_WITH_NEW_LINES + "]");
  private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile(" {2,}");

  public static String hideString(final String value) {
    return hideString(value, 1);
  }

  public static String hideString(final String value, int showChars) {
    if (showChars <= 0) {
      showChars = 1;
    }

    if (isEmpty(value) || value.length() <= 2 * showChars) {
      return value;
    }

    return value.substring(0, showChars)
        + "..."
        + value.length()
        + "..."
        + value.substring(value.length() - showChars);
  }

  public static String truncateValue(final String value) {
    return value != null && value.length() > MAX_VALUE_CHARS_IN_TO_STRING
        ? (StringUtils.truncate(value, MAX_VALUE_CHARS_IN_TO_STRING) + "...")
        : value;
  }

  public static UUID toUuid(final String value) {
    if (isEmpty(value)) {
      return null;
    }

    return UUID.fromString(value);
  }

  /**
   * Removes non-printable characters and line breaks, producing a trimmed single-line value.
   *
   * <p>Every whitespace character that has visual width or acts as a line/paragraph break (ASCII
   * whitespace, {@code NBSP}, the Unicode separators and line breaks matched by {@code (?U)\s}) is
   * replaced with a single space. The Unicode replacement character and any genuinely zero-width or
   * control characters (e.g. {@code U+200B}, {@code U+FEFF}, other {@code Cc}/{@code Cf} chars) are
   * dropped, consecutive spaces are collapsed and the result is trimmed. The kept character set
   * matches {@link #PRINTABLE_CHARS}.
   */
  public static String removeNonPrintableCharsAndNewLines(final String text) {
    if (isEmpty(text)) {
      return text;
    }

    String result = text.replace("�", "");
    result = WHITESPACE_PATTERN.matcher(result).replaceAll(" ");
    result = NON_PRINTABLE_PATTERN.matcher(result).replaceAll("");
    result = MULTIPLE_SPACES_PATTERN.matcher(result).replaceAll(" ");
    return result.trim();
  }

  /**
   * Removes non-printable characters while preserving line breaks, producing a trimmed multi-line
   * value.
   *
   * <p>Horizontal whitespace that has visual width (spaces, tabs, {@code NBSP} and the other
   * Unicode spaces) is normalized to a single space, while line breaks ({@code \r}, {@code \n}) are
   * kept. The Unicode replacement character and any genuinely zero-width or control characters
   * (e.g. {@code U+200B}, {@code U+FEFF}) are dropped, consecutive spaces are collapsed and the
   * result is trimmed. The kept character set matches {@link #PRINTABLE_CHARS_WITH_NEW_LINES}.
   */
  public static String removeNonPrintableCharsKeepNewLines(final String text) {
    if (isEmpty(text)) {
      return text;
    }

    String result = text.replace("�", "");
    result = HORIZONTAL_WHITESPACE_PATTERN.matcher(result).replaceAll(" ");
    result = NON_PRINTABLE_WITH_NEW_LINES_PATTERN.matcher(result).replaceAll("");
    result = MULTIPLE_SPACES_PATTERN.matcher(result).replaceAll(" ");
    return result.trim();
  }

  public static Set<String> filterNotBlank(final Collection<String> strings) {
    if (strings == null) {
      return Set.of();
    }

    return strings.stream()
        .map(StringUtils::trimToEmpty)
        .filter(StringUtils::isNotBlank)
        .collect(Collectors.toSet());
  }

  public static String concatNotEmpty(final String... strings) {
    return Arrays.stream(strings)
        .map(StringUtils::trimToEmpty)
        .filter(ObjectUtils::isNotEmpty)
        .collect(Collectors.joining(" "));
  }

  public static String zipDataToBase64(String data) {
    data = trimToEmpty(data);

    if (isEmpty(data)) {
      return "";
    }

    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

      try (final GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
        gzip.write(data.getBytes(UTF_8));
        gzip.flush();
      }

      return Base64.getEncoder().encodeToString(baos.toByteArray());

    } catch (final Exception e) {
      log.error("Failed to compress", e);

      return "<failed to compress>";
    }
  }

  public static String unzipBase64Data(String base64EncodedZip) {
    base64EncodedZip = trimToEmpty(base64EncodedZip);

    if (isEmpty(base64EncodedZip)) {
      return "";
    }

    final byte[] decodedBytes = Base64.getDecoder().decode(base64EncodedZip);

    try (final ByteArrayInputStream bais = new ByteArrayInputStream(decodedBytes)) {
      final GZIPInputStream gzip = new GZIPInputStream(bais);

      return IOUtils.toString(gzip, UTF_8);

    } catch (final Exception e) {
      log.error("Failed to decompress", e);

      return "<failed to decompress>";
    }
  }

  public static Object toZipOrPlainObject(final String json, final Object object) {
    return toZipOrPlainObject(json, object, LOG_MAX_DATA_LENGTH);
  }

  public static Object toZipOrPlainObject(
      String json, final Object object, final int logMaxDataLength) {
    json = trimToEmpty(json);

    if (isEmpty(json) || object == null) {
      return "";
    }

    if (json.length() > logMaxDataLength) {
      return zipDataToBase64(json);
    } else {
      return object;
    }
  }
}
