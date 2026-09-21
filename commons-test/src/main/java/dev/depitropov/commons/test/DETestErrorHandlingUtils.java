package dev.depitropov.commons.test;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.depitropov.commons.errors.BusinessRuleException;
import dev.depitropov.commons.errors.ErrorCode;
import dev.depitropov.commons.errors.ErrorDto;
import java.io.IOException;
import java.util.function.Consumer;
import org.opentest4j.AssertionFailedError;

public final class DETestErrorHandlingUtils {

  protected static final ObjectMapper objectMapper =
      new ObjectMapper().registerModule(new JavaTimeModule());

  public static BusinessRuleException assertBusinessRuleException(
      final Runnable codeToCheck, final ErrorCode errorCode) {
    return assertBusinessRuleException(codeToCheck, errorCode, "");
  }

  public static BusinessRuleException assertBusinessRuleException(
      final Runnable codeToCheck,
      final ErrorCode errorCode,
      final String errorMessage,
      final Object... args) {
    return assertBusinessRuleException(codeToCheck, errorCode, null, errorMessage, args);
  }

  public static BusinessRuleException assertBusinessRuleException(
      final Runnable codeToCheck,
      final ErrorCode errorCode,
      final Consumer<String> customCheckMessage) {
    return assertBusinessRuleException(codeToCheck, errorCode, customCheckMessage, null);
  }

  public static BusinessRuleException assertBusinessRuleException(
      final Runnable codeToCheck,
      final ErrorCode errorCode,
      final Consumer<String> customCheckMessage,
      final String errorMessage,
      final Object... args) {
    try {
      codeToCheck.run();

      fail("Should throw");
    } catch (final BusinessRuleException bre) {
      assertEquals(
          errorCode, bre.getErrorCode(), "Expected errorCode: " + errorCode + ", actual: " + bre);

      if (isNotEmpty(customCheckMessage)) {
        customCheckMessage.accept(bre.getMessage());
      }

      if (isNotEmpty(errorMessage)) {
        final String theMassageValue = String.format(errorMessage, args);
        assertEquals(
            "[" + errorCode + "]" + (theMassageValue.length() > 0 ? " " : "") + theMassageValue,
            bre.getMessage());
      }

      return bre;
    }
    return null;
  }

  public static void assertErrorDto(
      final String errorBody,
      final ErrorCode errorCode,
      final String errorMessage,
      final Object... args) {
    final ErrorDto errorDto;
    try {
      errorDto = objectMapper.readValue(errorBody, ErrorDto.class);
    } catch (final IOException e) {
      throw new AssertionFailedError("Failed to deserialize to ErrorDto body: " + errorBody, e);
    }
    assertEquals(
        errorCode.errorCode(), errorDto.getErrorCode(), "Unexpected errorCode, body: " + errorBody);
    assertEquals(
        errorCode.errorMessage(),
        errorDto.getErrorMessage(),
        "Unexpected errorMessage, body: " + errorBody);

    if (isNotEmpty(errorMessage)) {
      assertTrue(
          isNotEmpty(errorDto.getErrorDetails()),
          "Unexpected empty error details, body: " + errorBody);

      final String errDetail = errorDto.getErrorDetails().get(0);
      if (errDetail.contains("BusinessRuleException(")) {
        final String theMassageValue = String.format(errorMessage, args);
        assertEquals(
            "BusinessRuleException(["
                + errorCode
                + "]"
                + (theMassageValue.length() > 0 ? " " : "")
                + theMassageValue
                + ")",
            errDetail,
            "Unexpected BusinessRuleException in error details, body: " + errorBody);
      } else {
        assertEquals(
            String.format(errorMessage, args),
            errDetail,
            "Unexpected error details, body: " + errorBody);
      }
    }
  }
}
