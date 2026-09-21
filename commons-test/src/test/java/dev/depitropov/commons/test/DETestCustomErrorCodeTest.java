package dev.depitropov.commons.test;

import static dev.depitropov.commons.errors.GeneralErrorCode.BAD_REQUEST_GENERAL;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

@SuppressWarnings({"EqualsWithItself", "AssertBetweenInconvertibleTypes"})
class DETestCustomErrorCodeTest {

  @Test
  void testCustomErrorCode() {

    assertThat(new DETestCustomErrorCode(1001, "Error A"))
        .isEqualTo(new DETestCustomErrorCode(1001, "Error A"));

    assertThat(new DETestCustomErrorCode(1001, "Error A"))
        .isNotEqualTo(new DETestCustomErrorCode(1002, "Error A"));

    assertThat(new DETestCustomErrorCode(1001, "Error A"))
        .isNotEqualTo(new DETestCustomErrorCode(1001, "Error B"));

    assertThat(new DETestCustomErrorCode(4009999, "BAD_REQUEST_GENERAL"))
        .isEqualTo(BAD_REQUEST_GENERAL);
  }
}
