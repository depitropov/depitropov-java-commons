package dev.depitropov.commons;

import static dev.depitropov.commons.DEUuidUtils.DEFAULT_MAX_ID_SIZE;
import static dev.depitropov.commons.DEUuidUtils.base32ToUuid;
import static dev.depitropov.commons.DEUuidUtils.byteArrayToUuid;
import static dev.depitropov.commons.DEUuidUtils.isValidId;
import static dev.depitropov.commons.DEUuidUtils.uuidToBase32;
import static dev.depitropov.commons.DEUuidUtils.uuidToBase32Random;
import static dev.depitropov.commons.DEUuidUtils.uuidToBase32RandomLowerCase;
import static dev.depitropov.commons.DEUuidUtils.uuidToByteArray;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

class DEUuidUtilsTest {

  static final String TEST_UUID_STR = "50c56d29-92f9-356a-8800-2767ac7e21a2";

  static final UUID TEST_UUID = UUID.fromString(TEST_UUID_STR);

  static final String TEST_UUID_BASE32 = "KDCW2KMS7E2WVCAAE5T2Y7RBUI";

  static final byte[] TEST_UUID_BYTES =
      new byte[] {80, -59, 109, 41, -110, -7, 53, 106, -120, 0, 39, 103, -84, 126, 33, -94};

  @Test
  void uuidToBase32RandomTest() {
    final String randomBase32 = uuidToBase32Random();

    assertThat(randomBase32.length()).isGreaterThan(20);

    final UUID uuid = base32ToUuid(randomBase32);
    final UUID uuidL = base32ToUuid(randomBase32.toLowerCase());
    final UUID uuidU = base32ToUuid(randomBase32.toUpperCase());

    assertThat(uuidL).isEqualTo(uuid);
    assertThat(uuidU).isEqualTo(uuid);

    assertThat(uuidToBase32Random()).matches("^[A-Z0-9]+$");
    assertThat(uuidToBase32RandomLowerCase()).matches("^[a-z0-9]+$");
  }

  @Test
  void probabilityTest() {
    final int iterSize = 1_500_000;

    for (int i = 0; i < iterSize; i++) {
      final String randomBase32 = uuidToBase32Random();

      final UUID uuid = base32ToUuid(randomBase32);

      assertThat(uuidToBase32(uuid)).isEqualTo(randomBase32);

      final byte[] bytes = uuidToByteArray(uuid);
      final UUID uuidRes = byteArrayToUuid(bytes);

      assertThat(uuidRes).isEqualTo(uuid);
    }
  }

  @Test
  void base32ToUuidTest() {
    assertThat(base32ToUuid(TEST_UUID_BASE32)).isEqualTo(TEST_UUID);
  }

  @Test
  void uuidToBase32Test() {
    assertThat(uuidToBase32(TEST_UUID)).isEqualTo(TEST_UUID_BASE32);
  }

  @Test
  void uuidToByteArrayTest() {
    final byte[] bytes = uuidToByteArray(TEST_UUID);

    assertThat(bytes).isEqualTo(TEST_UUID_BYTES);
  }

  @Test
  void testIsValidId() {
    assertThat(isValidId(TEST_UUID_BASE32)).isTrue();
    assertThat(isValidId("C-" + TEST_UUID_BASE32)).isTrue();
    assertThat(isValidId("CD-" + TEST_UUID_BASE32)).isTrue();
    assertThat(isValidId("ABC-" + TEST_UUID_BASE32)).isTrue();
    assertThat(isValidId("LONG_LONG_PREFIX-" + TEST_UUID_BASE32)).isTrue();

    assertThat(
            isValidId(
                "VERY_LONG_LONG_PREFIX_LONG_LONG_PREFIX-"
                    + StringUtils.repeat('c', DEFAULT_MAX_ID_SIZE)
                    + TEST_UUID_BASE32))
        .isFalse();
    assertThat(
            isValidId(
                "VERY_LONG_LONG_PREFIX_LONG_LONG_PREFIX-"
                    + StringUtils.repeat('c', DEFAULT_MAX_ID_SIZE)
                    + TEST_UUID_BASE32,
                256))
        .isTrue();

    assertThat(isValidId("`" + TEST_UUID_BASE32)).isFalse();

    final List<String> INVALID_CHARS =
        List.of(
            " ", "\t", "\r", "\n", "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "+", "=", "\\",
            "|", "]", "[", "{", "}", ":", ";", "'", "\"", "<", ">", ",", ".", "?", "/", "~", "`");

    INVALID_CHARS.forEach(
        iter ->
            assertThat(isValidId(iter + TEST_UUID_BASE32))
                .withFailMessage("Should fail for char: " + iter)
                .isFalse());
  }
}
