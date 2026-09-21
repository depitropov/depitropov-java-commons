package dev.depitropov.commons.test;

import static dev.depitropov.commons.errors.BusinessRuleException.isValid;
import static dev.depitropov.commons.errors.GeneralErrorCode.BAD_REQUEST_GENERAL;
import static dev.depitropov.commons.errors.GeneralErrorCode.INTERNAL_SERVER_ERROR_GENERAL;
import static dev.depitropov.commons.errors.GeneralErrorCode.SERVICE_UNAVAILABLE_GENERAL;
import static dev.depitropov.commons.test.DETestErrorHandlingUtils.assertBusinessRuleException;
import static dev.depitropov.commons.test.DETestErrorHandlingUtils.assertErrorDto;
import static dev.depitropov.commons.test.DETestUtils.toJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.depitropov.commons.errors.BusinessRuleException;
import dev.depitropov.commons.errors.ErrorCode;
import dev.depitropov.commons.errors.ErrorDto;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

@SuppressWarnings("ThrowableNotThrown")
class DETestErrorHandlingUtilsTest {

  static final Instant TEST_INSTANT_20220905_081555_789 = Instant.ofEpochMilli(1652084155789L);

  @SuppressWarnings({"ConstantConditions"})
  @Test
  void assertBusinessRuleExceptionTest() {
    final BusinessRuleException bre =
        assertBusinessRuleException(
            () -> isValid(false, BAD_REQUEST_GENERAL, "Bad request: %d", 1),
            BAD_REQUEST_GENERAL,
            "Bad request: %d",
            1);

    assertThat(bre.getErrorCode()).isEqualTo(BAD_REQUEST_GENERAL);

    AssertionFailedError error =
        assertThrows(
            AssertionFailedError.class,
            () ->
                assertBusinessRuleException(
                    () ->
                        isValid(false, BAD_REQUEST_GENERAL, "Different message bad request: %d", 1),
                    BAD_REQUEST_GENERAL,
                    "Bad request: %d",
                    1));
    assertEquals(
        "expected: <[BAD_REQUEST_GENERAL] Bad request: 1> but was: "
            + "<[BAD_REQUEST_GENERAL] Different message bad request: 1>",
        error.getMessage());

    error =
        assertThrows(
            AssertionFailedError.class,
            () -> assertBusinessRuleException(() -> {}, BAD_REQUEST_GENERAL, "Bad request: %d", 1));
    assertEquals("Should throw", error.getMessage());
  }

  static class ErrorDtoInner extends ErrorDto {

    public ErrorDtoInner(
        final Instant timestamp, final ErrorCode errorCode, final List<String> errorDetails) {
      super(timestamp, errorCode, errorDetails, null);
    }

    String customErrorMessage;

    @Override
    public String getErrorMessage() {
      if (customErrorMessage != null) {
        return customErrorMessage;
      }

      return super.getErrorMessage();
    }
  }

  @Test
  void assertErrorDtoTest() {

    assertErrorDto(
        toJson(new ErrorDto(SERVICE_UNAVAILABLE_GENERAL, List.of("Test 1"))),
        SERVICE_UNAVAILABLE_GENERAL,
        "Test %d",
        1);

    assertErrorDto(
        toJson(
            new ErrorDto(
                SERVICE_UNAVAILABLE_GENERAL,
                List.of("BusinessRuleException([SERVICE_UNAVAILABLE_GENERAL] Test 1)"))),
        SERVICE_UNAVAILABLE_GENERAL,
        "Test %d",
        1);
  }

  @Test
  void assertErrorDtoTestInvalidJson() {

    final AssertionFailedError error =
        assertThrows(
            AssertionFailedError.class,
            () -> assertErrorDto("invalid", SERVICE_UNAVAILABLE_GENERAL, "Test %d", 1));

    assertEquals("Failed to deserialize to ErrorDto body: invalid", error.getMessage());
  }

  @Test
  void assertErrorDtoTestUnexpectedErrorCode() {

    final ErrorDto errorDto =
        new ErrorDtoInner(
            TEST_INSTANT_20220905_081555_789, INTERNAL_SERVER_ERROR_GENERAL, List.of("Test 1"));
    final String errorDtoJson = toJson(errorDto);

    final AssertionFailedError error =
        assertThrows(
            AssertionFailedError.class,
            () -> assertErrorDto(errorDtoJson, SERVICE_UNAVAILABLE_GENERAL, "Test %d", 1));

    assertEquals(
        "Unexpected errorCode, body: {\"timestamp\":\"2022-05-09T08:15:55.789Z\",\"httpStatus\":500,"
            + "\"errorCode\":5009999,\"errorMessage\":\"INTERNAL_SERVER_ERROR_GENERAL\","
            + "\"errorDetails\":[\"Test 1\"]} ==> expected: <5039999> but was: <5009999>",
        error.getMessage());
  }

  @Test
  void assertErrorDtoTestUnexpectedErrorMessage() {

    final ErrorDtoInner errorDto =
        new ErrorDtoInner(
            TEST_INSTANT_20220905_081555_789, SERVICE_UNAVAILABLE_GENERAL, List.of("Test 1"));

    errorDto.customErrorMessage = "INVALID_ERROR";

    final String errorDtoJson = toJson(errorDto);

    final AssertionFailedError error =
        assertThrows(
            AssertionFailedError.class,
            () -> assertErrorDto(errorDtoJson, SERVICE_UNAVAILABLE_GENERAL, "Test %d", 1));

    assertEquals(
        "Unexpected errorMessage, body: {\"timestamp\":\"2022-05-09T08:15:55.789Z\",\"httpStatus\":503,"
            + "\"errorCode\":5039999,\"errorMessage\":\"INVALID_ERROR\","
            + "\"errorDetails\":[\"Test 1\"]} ==> expected: <SERVICE_UNAVAILABLE_GENERAL> but was: <INVALID_ERROR>",
        error.getMessage());
  }

  @Test
  void assertErrorDtoTestUnexpectedErrorDetailsBRE() {

    final ErrorDtoInner errorDto =
        new ErrorDtoInner(
            TEST_INSTANT_20220905_081555_789,
            SERVICE_UNAVAILABLE_GENERAL,
            List.of("BusinessRuleException("));

    final String errorDtoJson = toJson(errorDto);

    final AssertionFailedError error =
        assertThrows(
            AssertionFailedError.class,
            () -> assertErrorDto(errorDtoJson, SERVICE_UNAVAILABLE_GENERAL, "Test %d", 1));

    assertEquals(
        "Unexpected BusinessRuleException in error details, body: {"
            + "\"timestamp\":\"2022-05-09T08:15:55.789Z\",\"httpStatus\":503,"
            + "\"errorCode\":5039999,\"errorMessage\":\"SERVICE_UNAVAILABLE_GENERAL\","
            + "\"errorDetails\":[\"BusinessRuleException(\"]} ==> "
            + "expected: <BusinessRuleException([SERVICE_UNAVAILABLE_GENERAL] Test 1)> "
            + "but was: <BusinessRuleException(>",
        error.getMessage());
  }

  @Test
  void assertErrorDtoTestUnexpectedErrorDetails() {

    final ErrorDtoInner errorDto =
        new ErrorDtoInner(
            TEST_INSTANT_20220905_081555_789, SERVICE_UNAVAILABLE_GENERAL, List.of("SomeDetails"));

    final String errorDtoJson = toJson(errorDto);

    final AssertionFailedError error =
        assertThrows(
            AssertionFailedError.class,
            () -> assertErrorDto(errorDtoJson, SERVICE_UNAVAILABLE_GENERAL, "Test %d", 1));

    assertEquals(
        "Unexpected error details, body: {\"timestamp\":\"2022-05-09T08:15:55.789Z\","
            + "\"httpStatus\":503,\"errorCode\":5039999,"
            + "\"errorMessage\":\"SERVICE_UNAVAILABLE_GENERAL\","
            + "\"errorDetails\":[\"SomeDetails\"]} ==> "
            + "expected: <Test 1> but was: <SomeDetails>",
        error.getMessage());
  }
}
