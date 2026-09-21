package dev.depitropov.commons.errors;

import static dev.depitropov.commons.DEResourceUtils.*;
import static dev.depitropov.commons.errors.ErrorUtils.extractBusinessRuleException;
import static dev.depitropov.commons.errors.ErrorUtils.findThrowable;
import static dev.depitropov.commons.errors.ErrorUtils.getExceptionClassNames;
import static dev.depitropov.commons.errors.ErrorUtils.getExceptionErrorInfo;
import static dev.depitropov.commons.errors.ErrorUtils.getExceptionFullClassNames;
import static dev.depitropov.commons.errors.ErrorUtils.getExceptionMessages;
import static dev.depitropov.commons.errors.ErrorUtils.getNotEmptyMessageOrExceptionClassSimpleName;
import static dev.depitropov.commons.errors.ErrorUtils.isAllowed;
import static dev.depitropov.commons.errors.ErrorUtils.isConflict;
import static dev.depitropov.commons.errors.ErrorUtils.isFoundResource;
import static dev.depitropov.commons.errors.ErrorUtils.isValidArgument;
import static dev.depitropov.commons.errors.ErrorUtils.registerExceptionMessageExtractors;
import static dev.depitropov.commons.errors.ErrorUtils.toBusinessRuleException;
import static dev.depitropov.commons.errors.ErrorUtils.trimAndValidateId;
import static dev.depitropov.commons.errors.GeneralErrorCode.BAD_REQUEST_GENERAL;
import static dev.depitropov.commons.errors.GeneralErrorCode.FORBIDDEN_GENERAL;
import static dev.depitropov.commons.errors.GeneralErrorCode.INTERNAL_SERVER_ERROR_GENERAL;
import static dev.depitropov.commons.errors.GeneralErrorCode.NOT_FOUND_GENERAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.*;

import java.io.IOException;
import java.net.ConnectException;
import java.time.Instant;
import java.util.List;
import javax.security.auth.login.AccountException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

class ErrorUtilsTest {

  @Test
  void findThrowableTest() {

    final IOException l2T = new IOException("io");
    final IllegalStateException l1T = new IllegalStateException(l2T);

    final BusinessRuleException l0T;
    l0T = new BusinessRuleException(l1T, BAD_REQUEST_GENERAL, "msg: %s", 2);

    assertSoftly(
        soft -> {
          soft.assertThat(findThrowable(l0T, BusinessRuleException.class)).isEqualTo(l0T);

          soft.assertThat(findThrowable(l0T, IllegalStateException.class)).isEqualTo(l1T);

          soft.assertThat(findThrowable(l0T, IOException.class)).isEqualTo(l2T);

          soft.assertThat(findThrowable(l0T, NullPointerException.class)).isNull();

          final ConnectException lc = new ConnectException("Connection refused");
          soft.assertThat(findThrowable(lc, ConnectException.class)).isEqualTo(lc);

          soft.assertThat(findThrowable(lc, IOException.class)).isEqualTo(lc);
        });
  }

  @Test
  void getExceptionMessagesTest() {

    assertSoftly(
        soft -> {
          soft.assertThat(getExceptionMessages(null)).isEqualTo(List.of());
          soft.assertThat(getExceptionMessages(new Throwable())).isEqualTo(List.of("Throwable"));

          soft.assertThat(getExceptionMessages(new Throwable("msg")))
              .isEqualTo(List.of("Throwable(msg)"));
          soft.assertThat(getExceptionMessages(new Throwable(""))).isEqualTo(List.of("Throwable"));

          soft.assertThat(getExceptionMessages(new Throwable(new IllegalStateException())))
              .isEqualTo(List.of("Throwable(IllegalStateException)", "IllegalStateException"));
          soft.assertThat(getExceptionMessages(new Throwable(new IllegalStateException(""))))
              .isEqualTo(List.of("Throwable(IllegalStateException)", "IllegalStateException"));
          soft.assertThat(getExceptionMessages(new Throwable(new IllegalStateException("state"))))
              .isEqualTo(
                  List.of("Throwable(IllegalStateException)", "IllegalStateException(state)"));

          soft.assertThat(
                  getExceptionMessages(
                      new IOException(
                          new IllegalStateException("state", new AccountException("not allowed")))))
              .isEqualTo(
                  List.of(
                      "IOException(IllegalStateException)",
                      "IllegalStateException(state)",
                      "AccountException(not allowed)"));

          soft.assertThat(
                  getExceptionMessages(
                      new IOException(
                          new IllegalStateException(new AccountException("not allowed")))))
              .isEqualTo(
                  List.of(
                      "IOException(IllegalStateException)",
                      "IllegalStateException(AccountException)",
                      "AccountException(not allowed)"));
        });
  }

  @Test
  void getExceptionMessagesTestNoAddClass() {

    assertSoftly(
        soft -> {
          soft.assertThat(getExceptionMessages(null, false)).isEqualTo(List.of());
          soft.assertThat(getExceptionMessages(new Throwable(), false))
              .isEqualTo(List.of("Throwable"));

          soft.assertThat(getExceptionMessages(new Throwable("msg"), false))
              .isEqualTo(List.of("msg"));
          soft.assertThat(getExceptionMessages(new Throwable(""), false))
              .isEqualTo(List.of("Throwable"));

          soft.assertThat(getExceptionMessages(new Throwable(new IllegalStateException()), false))
              .isEqualTo(List.of("IllegalStateException"));
          soft.assertThat(getExceptionMessages(new Throwable(new IllegalStateException("")), false))
              .isEqualTo(List.of("IllegalStateException"));
          soft.assertThat(
                  getExceptionMessages(new Throwable(new IllegalStateException("state")), false))
              .isEqualTo(List.of("IllegalStateException", "state"));

          soft.assertThat(
                  getExceptionMessages(
                      new IOException(
                          new IllegalStateException("state", new AccountException("not allowed"))),
                      false))
              .isEqualTo(List.of("IllegalStateException", "state", "not allowed"));

          soft.assertThat(
                  getExceptionMessages(
                      new IOException(
                          new IllegalStateException(new AccountException("not allowed"))),
                      false))
              .isEqualTo(List.of("IllegalStateException", "AccountException", "not allowed"));
        });
  }

  @Test
  void getExceptionClassNamesAndFullClassNamesTest() {

    assertSoftly(
        soft -> {
          soft.assertThat(getExceptionClassNames(null)).isEqualTo(List.of());
          soft.assertThat(getExceptionFullClassNames(null)).isEqualTo(List.of());
          soft.assertThat(getExceptionClassNames(new Throwable())).isEqualTo(List.of("Throwable"));
          soft.assertThat(getExceptionFullClassNames(new Throwable()))
              .isEqualTo(List.of("java.lang.Throwable"));

          soft.assertThat(getExceptionClassNames(new Throwable(new IllegalStateException())))
              .isEqualTo(List.of("Throwable", "IllegalStateException"));
          soft.assertThat(getExceptionFullClassNames(new Throwable(new IllegalStateException())))
              .isEqualTo(List.of("java.lang.Throwable", "java.lang.IllegalStateException"));

          soft.assertThat(
                  getExceptionClassNames(
                      new IOException(
                          new IllegalStateException("state", new AccountException("not allowed")))))
              .isEqualTo(List.of("IOException", "IllegalStateException", "AccountException"));
          soft.assertThat(
                  getExceptionFullClassNames(
                      new IOException(
                          new IllegalStateException("state", new AccountException("not allowed")))))
              .isEqualTo(
                  List.of(
                      "java.io.IOException",
                      "java.lang.IllegalStateException",
                      "javax.security.auth.login.AccountException"));
        });
  }

  @Test
  void getExceptionMessagesTestRegisterExceptionMessageExtractor() {

    registerExceptionMessageExtractors(List.of(new MsgExt1(), new MsgExt2()));

    assertSoftly(
        soft -> {
          soft.assertThat(
                  getExceptionMessages(new TestException1("y6", new TestException2("Y9")), false))
              .isEqualTo(List.of("1 custom message: y6", "2 custom message: Y9"));

          soft.assertThat(getExceptionMessages(new TestException2Ext("10"), false))
              .isEqualTo(List.of("2 custom message: 10"));

          soft.assertThat(
                  getExceptionErrorInfo(new TestException1("y6", new TestException2("Y9")), false)
                      .errorCodes())
              .isEqualTo(List.of(BAD_REQUEST_GENERAL, FORBIDDEN_GENERAL));
        });
  }

  @Test
  void selfCauseInChain() {

    assertThat(getExceptionMessages(new IOException(new ExceptionWithSelfCause("Self"))))
        .isEqualTo(
            List.of(
                "IOException(ExceptionWithSelfCause)",
                "ExceptionWithSelfCause(Self)",
                "IllegalStateException(internal self)"));
  }

  @Test
  void getNotEmptyMessageOrExceptionClassSimpleNameTest() {

    assertSoftly(
        soft -> {
          soft.assertThat(getNotEmptyMessageOrExceptionClassSimpleName(new Exception("msg")))
              .isEqualTo("msg");

          soft.assertThat(
                  getNotEmptyMessageOrExceptionClassSimpleName(new Exception((String) null)))
              .isEqualTo("Exception");

          soft.assertThat(getNotEmptyMessageOrExceptionClassSimpleName(new Exception("")))
              .isEqualTo("Exception");
        });
  }

  @SuppressWarnings("DataFlowIssue")
  @Test
  void isValidArgumentTest() {

    assertThatThrownBy(() -> isValidArgument(false, "Message"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("[BAD_REQUEST_GENERAL] Message");
  }

  @SuppressWarnings("DataFlowIssue")
  @Test
  void isFoundResourceTest() {

    assertThatThrownBy(() -> isFoundResource(false, "Message"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("[NOT_FOUND_GENERAL] Message");
  }

  @SuppressWarnings("DataFlowIssue")
  @Test
  void isAllowedTest() {

    assertThatThrownBy(() -> isAllowed(false, "Message"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("[FORBIDDEN_GENERAL] Message");
  }

  @SuppressWarnings("DataFlowIssue")
  @Test
  void isConflictTest() {

    assertThatThrownBy(() -> isConflict(false, "Message"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("[CONFLICT_GENERAL] Message");
  }

  @Test
  void trimAndValidateIdTest() {

    assertSoftly(
        soft -> {
          soft.assertThat(trimAndValidateId(" value\r\n\t ", "testId")).isEqualTo("value");
          soft.assertThat(
                  trimAndValidateId(
                      "Very-Long-Long-ID-Very-Long-Long-ID-Very-Long-Long-ID-Very-Long-Long-ID-Very-Long-Long-ID-",
                      "testId",
                      100))
              .isEqualTo(
                  "Very-Long-Long-ID-Very-Long-Long-ID-Very-Long-Long-ID-Very-Long-Long-ID-Very-Long-Long-ID-");

          soft.assertThatThrownBy(() -> trimAndValidateId(null, "testId"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[BAD_REQUEST_GENERAL] Expected not empty testId");
          soft.assertThatThrownBy(() -> trimAndValidateId("", "testId"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[BAD_REQUEST_GENERAL] Expected not empty testId");
          soft.assertThatThrownBy(() -> trimAndValidateId(" ", "testId"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[BAD_REQUEST_GENERAL] Expected not empty testId");
          soft.assertThatThrownBy(() -> trimAndValidateId(" \r\n\t", "testId"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[BAD_REQUEST_GENERAL] Expected not empty testId");

          soft.assertThatThrownBy(() -> trimAndValidateId("!@id==Wrong", "testId"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[BAD_REQUEST_GENERAL] Invalid testId: !@id==Wrong");

          final String longId =
              "Very-Long-Long-ID-Very-Long-Long-ID-Very-Long-Long-ID-Very-Long-Long-ID-Very-Long-Long-ID-"
                  + StringUtils.repeat("x", 100);
          soft.assertThatThrownBy(() -> trimAndValidateId(longId, "testId"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage(
                  "[BAD_REQUEST_GENERAL] Invalid testId: Very-Long-Long-ID-Very-Long-Long-ID-Very-Long-Long-ID-Very-Long-...190..."
                      + "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

          soft.assertThatThrownBy(() -> trimAndValidateId("!@id==Wrong", null))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Expected not empty idName");

          soft.assertThatThrownBy(() -> trimAndValidateId("!@id==Wrong", ""))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Expected not empty idName");

          soft.assertThatThrownBy(() -> trimAndValidateId("!@id==Wrong", "\r\n\t "))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Expected not empty idName");

          soft.assertThatThrownBy(() -> trimAndValidateId("!@id==Wrong", "idName", -1))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Expected maxIdSize to be positive: -1");

          soft.assertThatThrownBy(() -> trimAndValidateId("!@id==Wrong", "idName", 0))
              .isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Expected maxIdSize to be positive: 0");
        });
  }

  @Test
  void toBusinessRuleExceptionTest() {

    assertSoftly(
        soft -> {
          soft.assertThat(toBusinessRuleException(null))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[INTERNAL_SERVER_ERROR_GENERAL] Unknown");

          soft.assertThat(toBusinessRuleException(null, "Error message"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[INTERNAL_SERVER_ERROR_GENERAL] Error message");

          soft.assertThat(toBusinessRuleException(ErrorDto.builder().build(), "Error message"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[INTERNAL_SERVER_ERROR_GENERAL] Error message");

          soft.assertThat(
                  toBusinessRuleException(
                      ErrorDto.builder().errorCode(429).build(), "Error message"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[TOO_MANY_REQUESTS_GENERAL] Error message");

          soft.assertThat(
                  toBusinessRuleException(
                      ErrorDto.builder().httpStatus(429).build(), "Error message"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[TOO_MANY_REQUESTS_GENERAL] Error message");

          soft.assertThat(
                  toBusinessRuleException(
                      ErrorDto.builder().errorCode(4049999).build(), "Error message"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[NOT_FOUND_GENERAL] Error message");

          soft.assertThat(
                  toBusinessRuleException(
                      ErrorDto.builder().httpStatus(400).errorCode(1006).build(), "Error message"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[BAD_REQUEST_GENERAL] Error message");

          soft.assertThat(
                  toBusinessRuleException(
                      ErrorDto.builder().httpStatus(500).errorCode(1006).build(), "Error message"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[INTERNAL_SERVER_ERROR_GENERAL] Error message");

          soft.assertThat(
                  toBusinessRuleException(
                      ErrorDto.builder()
                          .errorCode(4041002)
                          .errorMessage("NOT_FOUND_CUSTOM")
                          .build(),
                      "Error message"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[NOT_FOUND_CUSTOM] Error message");

          soft.assertThat(
                  toBusinessRuleException(
                      ErrorDto.builder().errorCode(4041002).errorMessage("").build(),
                      "Error message"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[4041002] Error message");

          final IOException th = new IOException("error");
          BusinessRuleException res =
              toBusinessRuleException(
                  ErrorDto.builder().errorCode(700).errorMessage("ERROR_700").cause(th).build(),
                  "Error message");
          soft.assertThat(res)
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[INTERNAL_SERVER_ERROR_GENERAL] Error message");
          soft.assertThat(res.getCause()).isEqualTo(th);
          soft.assertThat(res.getErrorCode()).isEqualTo(INTERNAL_SERVER_ERROR_GENERAL);

          soft.assertThat(
                  toBusinessRuleException(
                      ErrorDto.builder()
                          .errorCode(4041002)
                          .errorMessage("NOT_FOUND_CUSTOM")
                          .errorDetails(List.of("error 1"))
                          .build(),
                      ""))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[NOT_FOUND_CUSTOM] error 1");

          soft.assertThat(
                  toBusinessRuleException(
                      ErrorDto.builder()
                          .errorCode(4041002)
                          .errorMessage("NOT_FOUND_CUSTOM")
                          .errorDetails(List.of("error 1", "error 2"))
                          .build(),
                      ""))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[NOT_FOUND_CUSTOM] error 1, error 2");

          soft.assertThat(toBusinessRuleException(400, ""))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[BAD_REQUEST_GENERAL]");

          soft.assertThat(toBusinessRuleException(400, null))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[BAD_REQUEST_GENERAL]");

          soft.assertThat(toBusinessRuleException(400, "Bad request"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[BAD_REQUEST_GENERAL] Bad request");

          res = toBusinessRuleException(400, loadResource("error-test1.json"));
          soft.assertThat(res)
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[BAD_REQUEST_GENERAL] error detail1, error detail 2");

          soft.assertThat(res.getTimestamp()).isEqualTo(Instant.parse("2022-05-09T08:15:55.789Z"));
          soft.assertThat(res.getErrorCode()).isEqualTo(GeneralErrorCode.BAD_REQUEST_GENERAL);
          soft.assertThat(res.getCause()).isNull();
          soft.assertThat(res.getMessage())
              .isEqualTo("[BAD_REQUEST_GENERAL] error detail1, error detail 2");

          soft.assertThat(
                  toBusinessRuleException(
                      401,
                      """
        {
          "error": "Error message custom"
        }
        """))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[UNAUTHORIZED_GENERAL] Error message custom");

          soft.assertThat(
                  toBusinessRuleException(
                      404,
                      """
        {
          "message": "Message custom"
        }
        """))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[NOT_FOUND_GENERAL] Message custom");

          soft.assertThat(
                  toBusinessRuleException(
                      404,
                      """
        {
          "error": "Error",
          "message": "Message custom"
        }
        """))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[NOT_FOUND_GENERAL] Error, Message custom");

          soft.assertThat(
                  toBusinessRuleException(
                      404,
                      """
        {
          "custom": "Custom",
          "message": "Message custom"
        }
        """))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage(
                  "[NOT_FOUND_GENERAL] "
                      + """
        {
          "custom": "Custom",
          "message": "Message custom"
        }""");

          soft.assertThat(toBusinessRuleException(null, "Error message %s"))
              .isInstanceOf(BusinessRuleException.class)
              .hasMessage("[INTERNAL_SERVER_ERROR_GENERAL] Error message %s");
        });
  }

  @Test
  void toBusinessRuleException_WithHttpStatusZeroInJson_ShouldUseParameterHttpStatus() {

    final String jsonWithZeroHttpStatus =
        """
        {
          "httpStatus": 0,
          "errorCode": 4009999,
          "errorMessage": "BAD_REQUEST_GENERAL",
          "errorDetails": ["Error detail"]
        }
        """;

    final BusinessRuleException result = toBusinessRuleException(400, jsonWithZeroHttpStatus);

    assertThat(result)
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("[BAD_REQUEST_GENERAL] Error detail");

    assertThat(result.getErrorCode()).isEqualTo(GeneralErrorCode.BAD_REQUEST_GENERAL);
  }

  @Test
  void extractBusinessRuleException_WithNullException_ShouldThrowNullPointerException() {

    assertThatThrownBy(
            () -> {
              throw extractBusinessRuleException(null, "Error message");
            })
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Exception is required");
  }

  @Test
  void extractBusinessRuleException_WithNullErrorMessage_ShouldThrowNullPointerException() {

    final RuntimeException re = new RuntimeException("test");

    assertThatThrownBy(
            () -> {
              throw extractBusinessRuleException(re, null);
            })
        .isInstanceOf(NullPointerException.class)
        .hasMessage("Error message is required");
  }

  @Test
  void extractBusinessRuleException_WithEmptyErrorMessage_ShouldThrowIllegalArgumentException() {

    final RuntimeException re = new RuntimeException("test");

    assertThatThrownBy(
            () -> {
              throw extractBusinessRuleException(re, "");
            })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Error message is required");
  }

  @Test
  void extractBusinessRuleException_WithBusinessRuleException_ShouldRethrowSameException() {
    final BusinessRuleException original =
        new BusinessRuleException(BAD_REQUEST_GENERAL, "Original error");

    assertThatThrownBy(
            () -> {
              throw extractBusinessRuleException(original, "New error message");
            })
        .isInstanceOf(BusinessRuleException.class)
        .isSameAs(original)
        .hasMessage("[BAD_REQUEST_GENERAL] Original error");
  }

  @Test
  void extractBusinessRuleException_WithBusinessRuleExceptionAsCause_ShouldRethrowCause() {

    final BusinessRuleException cause =
        new BusinessRuleException(NOT_FOUND_GENERAL, "Resource not found");

    final RuntimeException wrapper = new RuntimeException("Wrapper", cause);

    assertThatThrownBy(
            () -> {
              throw extractBusinessRuleException(wrapper, "New error message");
            })
        .isInstanceOf(BusinessRuleException.class)
        .isSameAs(cause)
        .hasMessage("[NOT_FOUND_GENERAL] Resource not found");
  }

  @Test
  void
      extractBusinessRuleException_WithBusinessRuleExceptionAsNestedCause_ShouldRethrowNestedCause() {

    final BusinessRuleException nestedCause =
        new BusinessRuleException(FORBIDDEN_GENERAL, "Access denied");

    final IllegalStateException intermediateCause =
        new IllegalStateException("Intermediate", nestedCause);
    final RuntimeException wrapper = new RuntimeException("Wrapper", intermediateCause);

    assertThatThrownBy(
            () -> {
              throw extractBusinessRuleException(wrapper, "New error message");
            })
        .isInstanceOf(BusinessRuleException.class)
        .isSameAs(nestedCause)
        .hasMessage("[FORBIDDEN_GENERAL] Access denied");
  }

  @Test
  void extractBusinessRuleException_WithRegularException_ShouldCreateNewBusinessRuleException() {

    final RuntimeException original = new RuntimeException("Original error");

    assertThatThrownBy(
            () -> {
              throw extractBusinessRuleException(
                  original, "Failed to process item. itemId: %s, userId: %s", "item123", "user456");
            })
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(
            "[INTERNAL_SERVER_ERROR_GENERAL] Failed to process item. itemId: item123, userId: user456")
        .hasCause(original);
  }

  @Test
  void extractBusinessRuleException_WithCustomErrorCode_ShouldUseProvidedErrorCode() {

    final IOException original = new IOException("IO error");

    assertThatThrownBy(
            () -> {
              throw extractBusinessRuleException(
                  original, NOT_FOUND_GENERAL, "Resource not available, var1: %s", 1);
            })
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("[NOT_FOUND_GENERAL] Resource not available, var1: 1")
        .hasCause(original);
  }

  static class ExceptionWithSelfCause extends RuntimeException {

    public ExceptionWithSelfCause(final String message) {
      super(message);

      initCause(new IllegalStateException("internal self", this));
    }
  }

  private static class MsgExt1 implements ExceptionMessageExtractor<TestException1> {

    @Override
    public Class<TestException1> getExceptionClass() {
      return TestException1.class;
    }

    @Override
    public ErrorCode getErrorCode(final Throwable throwable) {
      return BAD_REQUEST_GENERAL;
    }

    @Override
    public String getMessage(final Throwable throwable) {
      return ((TestException1) throwable).getCustomMessage();
    }
  }

  private static class TestException1 extends RuntimeException {

    private final String customMessage;

    private TestException1(final String customMessage, final Throwable cause) {
      super(cause);

      this.customMessage = customMessage;
    }

    public String getCustomMessage() {
      return "1 custom message: " + customMessage;
    }
  }

  private static class MsgExt2 implements ExceptionMessageExtractor<TestException2> {

    @Override
    public Class<TestException2> getExceptionClass() {
      return TestException2.class;
    }

    @Override
    public ErrorCode getErrorCode(final Throwable throwable) {
      return FORBIDDEN_GENERAL;
    }

    @Override
    public String getMessage(final Throwable throwable) {
      return ((TestException2) throwable).getCustomMessage();
    }
  }

  private static class TestException2 extends RuntimeException {

    private final String customMessage;

    private TestException2(final String customMessage) {
      this.customMessage = customMessage;
    }

    public String getCustomMessage() {
      return "2 custom message: " + customMessage;
    }
  }

  private class TestException2Ext extends TestException2 {

    private TestException2Ext(final String customMessage) {
      super(customMessage);
    }
  }
}
