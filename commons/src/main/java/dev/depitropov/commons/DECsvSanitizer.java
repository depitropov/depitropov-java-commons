package dev.depitropov.commons;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.stripToEmpty;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * Utility class for sanitizing CSV content to prevent CSV Injection (Formula Injection) attacks.
 *
 * <p>This class implements OWASP-recommended sanitization techniques to ensure that user-provided
 * data embedded in CSV files cannot be interpreted as formulas by spreadsheet applications like
 * Microsoft Excel, LibreOffice Calc, or Google Sheets.
 *
 * <p><b>Background:</b> CSV Injection occurs when cells beginning with special characters (=, @,
 * tab, CR, LF) are interpreted as formulas, potentially leading to code execution, data
 * exfiltration, or data manipulation.
 *
 * <p><b>Sanitization Approach:</b> Removes dangerous characters from the beginning of values:
 *
 * <ul>
 *   <li>Always removes: =, @, tab, carriage return, line feed
 *   <li>Preserves +/- when followed only by digits (e.g., "-100", "+50")
 *   <li>Removes +/- when followed by non-digits (e.g., "+cmd", "-attack")
 * </ul>
 *
 * <p><b>Example Usage:</b>
 *
 * <pre>
 * // Sanitize dangerous formula
 * String safe = DECsvSanitizer.sanitize("=1+2"); // Returns "1+2"
 *
 * // Preserve negative numbers
 * String number = DECsvSanitizer.sanitize("-100"); // Returns "-100"
 *
 * // Remove dangerous minus
 * String attack = DECsvSanitizer.sanitize("-cmd"); // Returns "cmd"
 *
 * </pre>
 *
 * @see <a href="https://owasp.org/www-community/attacks/CSV_Injection">OWASP CSV Injection</a>
 * @see <a href="https://cwe.mitre.org/data/definitions/1236.html">CWE-1236</a>
 */
@Slf4j
public final class DECsvSanitizer {

  /**
   * Sanitizes a single CSV cell value by removing dangerous characters from the beginning.
   *
   * <p>Dangerous characters that are always removed: =, @, tab, carriage return, line feed
   *
   * <p>Smart handling of +/- signs:
   *
   * <ul>
   *   <li>Preserves if followed only by digits (negative/positive numbers)
   *   <li>Removes if followed by non-digits (potential formula injection)
   * </ul>
   *
   * @param value the cell value to sanitize (can be null)
   * @return sanitized value safe for CSV export, or null if input was null
   */
  public static String sanitize(String value) {

    if (value == null) {
      return null;
    }

    value = stripToEmpty(value);

    if (isEmpty(value)) {
      return value;
    }

    int originalLength;

    do {
      originalLength = value.length();

      value = value.replaceAll("('\"|\"'|\"\"|'')", "\"");

    } while (isNotEmpty(value) && originalLength != value.length());

    do {
      originalLength = value.length();

      final char firstChar = value.charAt(0);

      if (firstChar == '=' || firstChar == '@') {
        value = value.substring(1);

        continue;
      }

      if (firstChar == '+' || firstChar == '-') {

        if (value.length() == 1) {
          value = "";

          break;
        }

        if (isZerosOnly(value.substring(1))) {
          value = "0";

          break;
        }

        if (NumberUtils.isCreatable(value.substring(1))) {
          break;
        }

        value = value.substring(1);
      }

    } while (isNotEmpty(value) && originalLength != value.length());

    return value;
  }

  private static boolean isZerosOnly(final String value) {

    for (int i = 0; i < value.length(); i++) {
      if (value.charAt(i) != '0') {

        return false;
      }
    }

    return true;
  }
}
