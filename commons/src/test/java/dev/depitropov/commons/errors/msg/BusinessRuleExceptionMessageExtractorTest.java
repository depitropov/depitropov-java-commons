package dev.depitropov.commons.errors.msg;

import static dev.depitropov.commons.errors.GeneralErrorCode.BAD_REQUEST_GENERAL;
import static org.assertj.core.api.Assertions.assertThat;

import dev.depitropov.commons.errors.BusinessRuleException;
import org.junit.jupiter.api.Test;

class BusinessRuleExceptionMessageExtractorTest {
  @Test
  void getMessage() {
    assertThat(
            new BusinessRuleExceptionMessageExtractor(false)
                .getMessage(
                    new BusinessRuleException(BAD_REQUEST_GENERAL, "Original message: %s", "arg1")))
        .isEqualTo("[BAD_REQUEST_GENERAL] Original message: arg1");

    assertThat(
            new BusinessRuleExceptionMessageExtractor(true)
                .getMessage(
                    new BusinessRuleException(BAD_REQUEST_GENERAL, "Original message: %s", "arg1")))
        .isEqualTo("Original message: arg1");
  }
}
