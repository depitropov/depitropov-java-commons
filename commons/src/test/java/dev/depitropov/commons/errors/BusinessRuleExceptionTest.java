package dev.depitropov.commons.errors;

import static dev.depitropov.commons.errors.BusinessRuleException.isValid;
import static dev.depitropov.commons.errors.GeneralErrorCode.INTERNAL_SERVER_ERROR_GENERAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class BusinessRuleExceptionTest {

  @Test
  void isValidTest() {
    final BusinessRuleException err =
        assertThrows(
            BusinessRuleException.class,
            () -> isValid(false, INTERNAL_SERVER_ERROR_GENERAL, "Error message: %s", "arg"));
    assertEquals("[INTERNAL_SERVER_ERROR_GENERAL] Error message: arg", err.getMessage());
    assertEquals(INTERNAL_SERVER_ERROR_GENERAL, err.getErrorCode());
  }

  @Test
  void testWithCause() {
    final Throwable cause = new IllegalStateException("state");

    final BusinessRuleException err =
        assertThrows(
            BusinessRuleException.class,
            () -> {
              throw new BusinessRuleException(
                  cause, INTERNAL_SERVER_ERROR_GENERAL, "Error message: %s", "arg");
            });
    assertEquals("[INTERNAL_SERVER_ERROR_GENERAL] Error message: arg", err.getMessage());
    assertEquals(INTERNAL_SERVER_ERROR_GENERAL, err.getErrorCode());
    assertEquals(cause, err.getCause());
  }
}
