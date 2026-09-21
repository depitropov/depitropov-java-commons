package dev.depitropov.commons;

import static dev.depitropov.commons.DEStringUtils.LOG_MAX_DATA_LENGTH;
import static dev.depitropov.commons.DEStringUtils.PRINTABLE_CHARS;
import static dev.depitropov.commons.DEStringUtils.PRINTABLE_CHARS_WITH_NEW_LINES;
import static dev.depitropov.commons.DEStringUtils.concatNotEmpty;
import static dev.depitropov.commons.DEStringUtils.filterNotBlank;
import static dev.depitropov.commons.DEStringUtils.hideString;
import static dev.depitropov.commons.DEStringUtils.removeNonPrintableCharsAndNewLines;
import static dev.depitropov.commons.DEStringUtils.removeNonPrintableCharsKeepNewLines;
import static dev.depitropov.commons.DEStringUtils.toUuid;
import static dev.depitropov.commons.DEStringUtils.toZipOrPlainObject;
import static dev.depitropov.commons.DEStringUtils.truncateValue;
import static dev.depitropov.commons.DEStringUtils.unzipBase64Data;
import static dev.depitropov.commons.DEStringUtils.zipDataToBase64;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DEStringUtilsTest {

  @Test
  void hideStringTest() {
    assertNull(hideString(null));
    assertThat(hideString("")).isEmpty();

    assertThat(hideString("a")).isEqualTo("a");
    assertThat(hideString("ab")).isEqualTo("ab");
    assertThat(hideString("abc")).isEqualTo("a...3...c");

    assertThat(hideString("abcdefg", 2)).isEqualTo("ab...7...fg");
  }

  @Test
  void truncateValueTest() {
    assertThat(
            truncateValue(
                "LongLongValue1 LongLongValue2 LongLongValue3 "
                    + "LongLongValue4 LongLongValue5 LongLongValue6 "
                    + "LongLongValue7 LongLongValue8 LongLongValue9"))
        .isEqualTo(
            "LongLongValue1 LongLongValue2 LongLongValue3 LongLongValue4 "
                + "LongLongValue5 LongLongValue6 LongLongValue7 LongLongValue8 LongLong...");
  }

  @Test
  void toUuidTest() {
    final UUID uuid1 = UUID.randomUUID();

    assertThat(toUuid(uuid1.toString())).isEqualTo(uuid1);
  }

  @Test
  void removeNonPrintableCharsAndNewLinesTest() {
    assertThat(removeNonPrintableCharsAndNewLines("Acme\r\nLLC")).isEqualTo("Acme LLC");
    assertThat(removeNonPrintableCharsAndNewLines("Acme\n\n  LLC")).isEqualTo("Acme LLC");
    assertThat(removeNonPrintableCharsAndNewLines("  \n Acme LLC \n ")).isEqualTo("Acme LLC");

    assertThat(removeNonPrintableCharsAndNewLines("Acme\tLLC")).isEqualTo("Acme LLC");
    assertThat(removeNonPrintableCharsAndNewLines("Acme  LLC")).isEqualTo("Acme LLC");

    assertThat(removeNonPrintableCharsAndNewLines("call\u200Bme")).isEqualTo("callme");
    assertThat(removeNonPrintableCharsAndNewLines("9-5\u0007")).isEqualTo("9-5");

    assertThat(removeNonPrintableCharsAndNewLines("Acme\uFFFD LLC")).isEqualTo("Acme LLC");

    assertThat(removeNonPrintableCharsAndNewLines("C++ & Co.")).isEqualTo("C++ & Co.");

    assertThat(removeNonPrintableCharsAndNewLines("雙賣 Ρļ")).isEqualTo("雙賣 Ρļ");

    assertNull(removeNonPrintableCharsAndNewLines(null));

    assertThat(removeNonPrintableCharsAndNewLines("")).isEmpty();

    assertThat(removeNonPrintableCharsAndNewLines(" \t\n ")).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("nonPrintableCodePoints")
  void removeNonPrintableCharsAndNewLines_StripsEachNonPrintableChar(final int codePoint) {
    final String nonPrintable = Character.toString(codePoint);

    final String cleaned = removeNonPrintableCharsAndNewLines("Acme" + nonPrintable + "Corp");

    assertThat(cleaned).isEqualTo("AcmeCorp").matches("[" + PRINTABLE_CHARS + "]*");
  }

  @ParameterizedTest
  @MethodSource("whitespaceLikeCodePoints")
  void removeNonPrintableCharsAndNewLines_ConvertsWhitespaceLikeToSingleSpace(final int codePoint) {
    final String whitespaceLike = Character.toString(codePoint);

    assertThat(removeNonPrintableCharsAndNewLines("Acme" + whitespaceLike + "Corp"))
        .isEqualTo("Acme Corp");
  }

  // Genuinely non-printable code points that must be deleted (no visual width, no line break).
  // U+0085 (NEL) is excluded: it is whitespace and becomes a space (see whitespaceLikeCodePoints).
  private static IntStream nonPrintableCodePoints() {
    return IntStream.concat(
            IntStream.concat(
                IntStream.concat(
                    IntStream.rangeClosed(0x00, 0x08), IntStream.rangeClosed(0x0E, 0x1F)),
                IntStream.concat(IntStream.of(0x7F), IntStream.rangeClosed(0x80, 0x9F))),
            IntStream.of(0x200B, 0x200C, 0x200D, 0xFEFF, 0xFFFD))
        .filter(codePoint -> codePoint != 0x85);
  }

  @Test
  void removeNonPrintableCharsKeepNewLinesTest() {
    assertThat(removeNonPrintableCharsKeepNewLines("line1\nline2")).isEqualTo("line1\nline2");
    assertThat(removeNonPrintableCharsKeepNewLines("line1\r\nline2")).isEqualTo("line1\r\nline2");

    assertThat(removeNonPrintableCharsKeepNewLines("a\t\tb\nc\u00A0d")).isEqualTo("a b\nc d");

    assertThat(removeNonPrintableCharsKeepNewLines("call\u200Bme")).isEqualTo("callme");

    assertThat(removeNonPrintableCharsKeepNewLines("price: $5 + tax")).isEqualTo("price: $5 + tax");

    assertThat(removeNonPrintableCharsKeepNewLines("雙賣 Ρļ")).isEqualTo("雙賣 Ρļ");

    assertNull(removeNonPrintableCharsKeepNewLines(null));

    assertThat(removeNonPrintableCharsKeepNewLines("")).isEmpty();

    assertThat(removeNonPrintableCharsKeepNewLines(" \t ")).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("nonPrintableCodePoints")
  void removeNonPrintableCharsKeepNewLines_StripsEachNonPrintableChar(final int codePoint) {
    final String nonPrintable = Character.toString(codePoint);

    final String cleaned = removeNonPrintableCharsKeepNewLines("Acme" + nonPrintable + "Corp");

    assertThat(cleaned).isEqualTo("AcmeCorp").matches("[" + PRINTABLE_CHARS_WITH_NEW_LINES + "]*");
  }

  @ParameterizedTest
  @MethodSource("horizontalWhitespaceLikeCodePoints")
  void removeNonPrintableCharsKeepNewLines_ConvertsHorizontalWhitespaceToSingleSpace(
      final int codePoint) {
    final String whitespaceLike = Character.toString(codePoint);

    assertThat(removeNonPrintableCharsKeepNewLines("Acme" + whitespaceLike + "Corp"))
        .isEqualTo("Acme Corp");
  }

  @ParameterizedTest
  @MethodSource("lineBreakCodePoints")
  void removeNonPrintableCharsKeepNewLines_PreservesLineBreaks(final int codePoint) {
    final String lineBreak = Character.toString(codePoint);

    assertThat(removeNonPrintableCharsKeepNewLines("Acme" + lineBreak + "Corp"))
        .isEqualTo("Acme" + lineBreak + "Corp");
  }

  // The full set matched by (?U)\s: ASCII whitespace, NEL, NBSP, OGHAM SPACE MARK, the
  // U+2000-U+200A block, LINE/PARAGRAPH SEPARATOR, NARROW/MEDIUM MATHEMATICAL SPACE, IDEOGRAPHIC
  // SPACE.
  private static IntStream whitespaceLikeCodePoints() {
    return IntStream.concat(
        IntStream.of(0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x20, 0x85, 0xA0, 0x1680),
        IntStream.concat(
            IntStream.rangeClosed(0x2000, 0x200A),
            IntStream.of(0x2028, 0x2029, 0x202F, 0x205F, 0x3000)));
  }

  // Same set minus \n (0x0A) and \r (0x0D), which the keep-new-lines variant preserves.
  private static IntStream horizontalWhitespaceLikeCodePoints() {
    return whitespaceLikeCodePoints().filter(codePoint -> codePoint != 0x0A && codePoint != 0x0D);
  }

  private static IntStream lineBreakCodePoints() {
    return IntStream.of(0x0A, 0x0D);
  }

  @Test
  void filterNotBlankTest() {
    assertThat(filterNotBlank(null)).isEmpty();
    assertThat(filterNotBlank(Set.of())).isEmpty();

    assertThat(filterNotBlank(Set.of("", "b", "  ", "\r\t\n  ", "\r\t\n  atest")))
        .isEqualTo(Set.of("atest", "b"));
    assertThat(filterNotBlank(List.of("", "b", "  ", "\r\t\n  ", "\ratest\t\r\n  \t\n")))
        .isEqualTo(Set.of("atest", "b"));
  }

  @Test
  void concatNotEmptyTest() {
    assertThat(concatNotEmpty(null, null)).isEmpty();
    assertThat(concatNotEmpty(null, "\r\n\t")).isEmpty();
    assertThat(concatNotEmpty("  ", null)).isEmpty();
    assertThat(concatNotEmpty("  \r\t", "\r\t t", "")).isEqualTo("t");
    assertThat(concatNotEmpty("a\r\n\t", null, "leon")).isEqualTo("a leon");
    assertThat(concatNotEmpty("\r\t\na  ", " b ")).isEqualTo("a b");
  }

  @Test
  void zipDataToBase64Test() {

    assertThat(zipDataToBase64(null)).isEmpty();
    assertThat(zipDataToBase64("")).isEmpty();
    assertThat(zipDataToBase64("\r\n \r\n \t")).isEmpty();

    assertThat(unzipBase64Data(zipDataToBase64("{\"id\": \"test\"}\r\n")))
        .isEqualTo("{\"id\": \"test\"}");
  }

  @Test
  void toZipOrPlainObjectTest() {
    final Object obj = new Object();

    assertThat(toZipOrPlainObject(null, obj)).isEqualTo("");
    assertThat(toZipOrPlainObject("", obj)).isEqualTo("");
    assertThat(toZipOrPlainObject("\r\n \r\n \t", obj)).isEqualTo("");
    assertThat(toZipOrPlainObject("{}", null)).isEqualTo("");

    assertThat(toZipOrPlainObject("{\"id\": \"test\"}\r\n", obj)).isEqualTo(obj);

    final String bigJson = "{\"id\": \"test" + StringUtils.repeat("r", LOG_MAX_DATA_LENGTH) + "\"}";

    assertThat(unzipBase64Data((String) toZipOrPlainObject(bigJson, obj))).isEqualTo(bigJson);
  }
}
