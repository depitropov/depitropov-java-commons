package dev.depitropov.commons.errors;

import static dev.depitropov.commons.errors.GeneralErrorCode.*;
import static dev.depitropov.commons.errors.GeneralErrorCode.BAD_REQUEST_GENERAL;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.junit.jupiter.api.Test;

class GeneralErrorCodeTest {

  @Test
  void testHttpStatusFromValue() {
    assertSoftly(
        soft -> {
          int httpStatus = httpStatusFromValue(4001234, false);
          soft.assertThat(httpStatus).isEqualTo(400);

          httpStatus = httpStatusFromValue(4041234);
          soft.assertThat(httpStatus).isEqualTo(404);

          httpStatus = httpStatusFromValue(441234, false);
          soft.assertThat(httpStatus).isEqualTo(500);

          httpStatus = httpStatusFromValue(441234);
          soft.assertThat(httpStatus).isEqualTo(500);

          httpStatus = httpStatusFromValue(6001234);
          soft.assertThat(httpStatus).isEqualTo(500);

          httpStatus = httpStatusFromValue(429);
          soft.assertThat(httpStatus).isEqualTo(429);

          soft.assertThatThrownBy(() -> httpStatusFromValue(441234, true))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Invalid HttpStatus: 44 from errorCode: 441234");

          soft.assertThatThrownBy(() -> httpStatusFromValue(7001234, true))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Invalid HttpStatus: 700 from errorCode: 7001234");

          soft.assertThat(httpStatusFromValueCheck(7001234, false)).isNull();

          soft.assertThat(httpStatusFromValueCheck(10006, false)).isNull();
        });
  }

  @Test
  void testFrom() {

    assertSoftly(
        soft -> {
          soft.assertThat(from(4009999)).isEqualTo(BAD_REQUEST_GENERAL);

          soft.assertThat(from(4049999)).isEqualTo(NOT_FOUND_GENERAL);

          soft.assertThat(from(449999)).isEqualTo(INTERNAL_SERVER_ERROR_GENERAL);

          soft.assertThat(from(6009999)).isEqualTo(INTERNAL_SERVER_ERROR_GENERAL);

          soft.assertThat(from(429)).isEqualTo(TOO_MANY_REQUESTS_GENERAL);
        });
  }
}
