package dev.depitropov.commons.temporal;

import static dev.depitropov.commons.errors.GeneralErrorCode.*;
import static dev.depitropov.commons.temporal.TemporalUtils.*;
import static io.temporal.api.enums.v1.RetryState.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.Timestamp;
import dev.depitropov.commons.errors.BusinessRuleException;
import dev.depitropov.commons.errors.GeneralErrorCode;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.EventType;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.api.workflowservice.v1.WorkflowServiceGrpc;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.common.SearchAttributeKey;
import io.temporal.common.SearchAttributes;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.ChildWorkflowFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Instant;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TemporalUtilsTest {

  public static final String TEST_WORKFLOW_ID = "testWorkflowId";

  @Test
  void nonRetryableFailureCheckTest() {
    assertThatThrownBy(
            () ->
                nonRetryableFailureCheck(
                    () -> {
                      throw new IllegalArgumentException("Test exception");
                    }))
        .isInstanceOf(ApplicationFailure.class)
        .hasMessage("message='Test exception', type='IllegalArgumentException', nonRetryable=true");

    assertThatThrownBy(
            () ->
                nonRetryableFailureCheck(
                    () -> {
                      throw new IllegalStateException("Test exception");
                    }))
        .isInstanceOf(ApplicationFailure.class)
        .hasMessage("message='Test exception', type='IllegalStateException', nonRetryable=true");

    assertThatThrownBy(
            () ->
                nonRetryableFailureCheck(
                    () -> {
                      throw new BusinessRuleException(BAD_REQUEST_GENERAL, "Test exception");
                    }))
        .isInstanceOf(ApplicationFailure.class)
        .hasMessage(
            "message='[BAD_REQUEST_GENERAL] Test exception', type='BusinessRuleException', "
                + "nonRetryable=true");

    assertThatThrownBy(
            () ->
                nonRetryableFailureCheck(
                    () -> {
                      throw new BusinessRuleException(
                          INTERNAL_SERVER_ERROR_GENERAL, "Test exception");
                    }))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("[INTERNAL_SERVER_ERROR_GENERAL] Test exception");

    assertThatThrownBy(
            () ->
                nonRetryableFailureCheck(
                    () -> {
                      throw new BusinessRuleException(TOO_MANY_REQUESTS_GENERAL, "Test exception");
                    }))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("[TOO_MANY_REQUESTS_GENERAL] Test exception");

    assertThatThrownBy(
            () ->
                nonRetryableFailureCheck(
                    () -> {
                      throw new UnsupportedOperationException("Unsupported test exception");
                    }))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("Unsupported test exception");
  }

  @ParameterizedTest
  @MethodSource("isNonRetryableFailureBusinessRuleExceptionArgs")
  void isNonRetryableFailureBusinessRuleExceptionTest(
      final GeneralErrorCode errorCode, final boolean expected) {
    final BusinessRuleException e = new BusinessRuleException(errorCode, "Test exception");

    assertThat(isNonRetryableFailure(e)).isEqualTo(expected);
  }

  private static Stream<Arguments> isNonRetryableFailureBusinessRuleExceptionArgs() {
    return Stream.of(
        Arguments.of(BAD_REQUEST_GENERAL, true),
        Arguments.of(FORBIDDEN_GENERAL, true),
        Arguments.of(NOT_FOUND_GENERAL, true),
        Arguments.of(UNPROCESSABLE_ENTITY_GENERAL, true),
        Arguments.of(REQUEST_TIMEOUT_GENERAL, false),
        Arguments.of(TOO_MANY_REQUESTS_GENERAL, false),
        Arguments.of(INTERNAL_SERVER_ERROR_GENERAL, false),
        Arguments.of(BAD_GATEWAY_GENERAL, false),
        Arguments.of(SERVICE_UNAVAILABLE_GENERAL, false));
  }

  @ParameterizedTest
  @MethodSource("isNonRetryableFailureExceptionTypeArgs")
  void isNonRetryableFailureExceptionTypeTest(final Exception e, final boolean expected) {
    assertThat(isNonRetryableFailure(e)).isEqualTo(expected);
  }

  private static Stream<Arguments> isNonRetryableFailureExceptionTypeArgs() {
    return Stream.of(
        Arguments.of(new IllegalArgumentException("Test exception"), true),
        Arguments.of(new IllegalStateException("Test exception"), true),
        Arguments.of(new RuntimeException("Test exception"), false));
  }

  @Test
  void nonRetryableFailureCheckPredicateTest() {
    final Predicate<Exception> predicate =
        e -> e instanceof RuntimeException && e.getMessage().equals("Test exception");

    assertThatThrownBy(
            () ->
                nonRetryableFailureCheck(
                    () -> {
                      throw new RuntimeException("Test exception");
                    },
                    predicate))
        .isInstanceOf(ApplicationFailure.class)
        .hasMessage("message='Test exception', type='RuntimeException', nonRetryable=true");

    assertThatThrownBy(
            () ->
                nonRetryableFailureCheck(
                    () -> {
                      throw new UnsupportedOperationException("Unsupported test exception");
                    },
                    predicate))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("Unsupported test exception");
  }

  @Test
  void buildSearchAttributesTest() {
    assertThat(buildSearchAttributes("companyId", null))
        .withFailMessage("User ID null")
        .isEqualTo(
            SearchAttributes.newBuilder()
                .set(SearchAttributeKey.forText(SEARCH_ATTR_KEY_COMPANY_ID), "companyId")
                .build());

    assertThat(buildSearchAttributes("companyId", ""))
        .withFailMessage("User ID empty")
        .isEqualTo(
            SearchAttributes.newBuilder()
                .set(SearchAttributeKey.forText(SEARCH_ATTR_KEY_COMPANY_ID), "companyId")
                .build());

    assertThat(buildSearchAttributes("companyId", "\r\n"))
        .withFailMessage("User ID trim")
        .isEqualTo(
            SearchAttributes.newBuilder()
                .set(SearchAttributeKey.forText(SEARCH_ATTR_KEY_COMPANY_ID), "companyId")
                .build());

    assertThat(buildSearchAttributes("companyId", "userId"))
        .isEqualTo(
            SearchAttributes.newBuilder()
                .set(SearchAttributeKey.forText(SEARCH_ATTR_KEY_COMPANY_ID), "companyId")
                .set(SearchAttributeKey.forText(SEARCH_ATTR_KEY_USER_ID), "userId")
                .build());

    assertThatThrownBy(() -> buildSearchAttributes(null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("companyId");
  }

  @Test
  void buildWorkflowExecutionTimeTest() {
    final Timestamp startTime = Timestamp.newBuilder().setSeconds(1000).setNanos(500).build();
    final Timestamp executionTime = Timestamp.newBuilder().setSeconds(2000).setNanos(500).build();
    final Timestamp closeTime = Timestamp.newBuilder().setSeconds(3000).setNanos(500).build();

    WorkflowExecutionInfo workflowExecutionInfo =
        WorkflowExecutionInfo.newBuilder()
            .setStartTime(startTime)
            .setExecutionTime(executionTime)
            .setCloseTime(closeTime)
            .build();

    WorkflowExecutionTime result = TemporalUtils.buildWorkflowExecutionTime(workflowExecutionInfo);

    assertThat(result.startTime()).isEqualTo(Instant.ofEpochSecond(1000, 500));
    assertThat(result.executionTime()).isEqualTo(Instant.ofEpochSecond(2000, 500));
    assertThat(result.closeTime()).isEqualTo(Instant.ofEpochSecond(3000, 500));

    workflowExecutionInfo = WorkflowExecutionInfo.newBuilder().setStartTime(startTime).build();

    result = TemporalUtils.buildWorkflowExecutionTime(workflowExecutionInfo);

    assertThat(result.startTime()).isEqualTo(Instant.ofEpochSecond(1000, 500));
    assertThat(result.executionTime()).isNull();
    assertThat(result.closeTime()).isNull();

    workflowExecutionInfo =
        WorkflowExecutionInfo.newBuilder()
            .setStartTime(startTime)
            .setExecutionTime(executionTime)
            .build();

    result = TemporalUtils.buildWorkflowExecutionTime(workflowExecutionInfo);

    assertThat(result.startTime()).isEqualTo(Instant.ofEpochSecond(1000, 500));
    assertThat(result.executionTime()).isEqualTo(Instant.ofEpochSecond(2000, 500));
    assertThat(result.closeTime()).isNull();

    workflowExecutionInfo = WorkflowExecutionInfo.newBuilder().build();

    result = TemporalUtils.buildWorkflowExecutionTime(workflowExecutionInfo);

    assertThat(result.startTime()).isNull();
    assertThat(result.executionTime()).isNull();
    assertThat(result.closeTime()).isNull();
  }

  @Test
  void isWorkflowRunningTest() {
    final WorkflowClient workflowClient = mock(WorkflowClient.class, RETURNS_DEEP_STUBS);

    when(workflowClient.getOptions().getNamespace()).thenReturn("testNamespace");

    final WorkflowServiceStubs workflowServiceStubs =
        mock(WorkflowServiceStubs.class, RETURNS_DEEP_STUBS);
    final WorkflowServiceGrpc.WorkflowServiceBlockingStub stub =
        mock(WorkflowServiceGrpc.WorkflowServiceBlockingStub.class, RETURNS_DEEP_STUBS);
    final DescribeWorkflowExecutionResponse response =
        mock(DescribeWorkflowExecutionResponse.class, RETURNS_DEEP_STUBS);

    when(workflowServiceStubs.blockingStub()).thenReturn(stub);
    when(stub.describeWorkflowExecution(any())).thenReturn(response);
    when(response.getWorkflowExecutionInfo().getStatus())
        .thenReturn(WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING);

    final boolean result =
        TemporalUtils.isWorkflowRunning(TEST_WORKFLOW_ID, workflowClient, workflowServiceStubs);

    assertThat(result).isTrue();
    verify(workflowServiceStubs).blockingStub();
    verify(stub).describeWorkflowExecution(any());
    verify(response.getWorkflowExecutionInfo()).getStatus();
  }

  @Test
  void isWorkflowRunningNotFoundTest() {
    final WorkflowClient workflowClient = mock(WorkflowClient.class, RETURNS_DEEP_STUBS);

    when(workflowClient.getOptions().getNamespace()).thenReturn("testNamespace");

    final WorkflowServiceStubs workflowServiceStubs =
        mock(WorkflowServiceStubs.class, RETURNS_DEEP_STUBS);
    final WorkflowServiceGrpc.WorkflowServiceBlockingStub stub =
        mock(WorkflowServiceGrpc.WorkflowServiceBlockingStub.class, RETURNS_DEEP_STUBS);
    final StatusRuntimeException statusRuntimeException =
        new StatusRuntimeException(Status.NOT_FOUND);

    when(workflowServiceStubs.blockingStub()).thenReturn(stub);
    when(stub.describeWorkflowExecution(any())).thenThrow(statusRuntimeException);

    final boolean result =
        TemporalUtils.isWorkflowRunning(TEST_WORKFLOW_ID, workflowClient, workflowServiceStubs);

    assertThat(result).isFalse();
    verify(workflowServiceStubs).blockingStub();
    verify(stub).describeWorkflowExecution(any());
  }

  @Test
  void extractTemporalErrorsAsStringTest() {
    final Throwable throwable =
        ApplicationFailure.newNonRetryableFailure("Test exception", "TestType");

    assertThat(TemporalUtils.extractTemporalErrorsAsString(throwable))
        .isEqualTo("TestType: Test exception");
  }

  @Test
  void extractTemporalErrorsBusinessRuleExceptionAsStringTest() {
    final Throwable throwable =
        ApplicationFailure.newNonRetryableFailure("Test exception", "BusinessRuleException");

    assertThat(TemporalUtils.extractTemporalErrorsAsString(throwable)).isEqualTo("Test exception");
  }

  @Test
  void extractTemporalErrorsTest() {
    final Throwable throwable =
        ApplicationFailure.newNonRetryableFailure("Test exception", "TestType");

    final List<String> result = TemporalUtils.extractTemporalErrors(throwable);

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo("TestType: Test exception");
  }

  @Test
  void extractTemporalErrorsWithWorkflowExceptionTest() {
    final Throwable cause = ApplicationFailure.newNonRetryableFailure("Test exception", "TestType");
    final Throwable throwable =
        new WorkflowFailedException(
            mock(WorkflowExecution.class),
            "workflowType",
            EventType.EVENT_TYPE_CHILD_WORKFLOW_EXECUTION_FAILED,
            1,
            RETRY_STATE_IN_PROGRESS,
            cause);
    final List<String> result = TemporalUtils.extractTemporalErrors(throwable);

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo("TestType: Test exception");
  }

  @Test
  void extractTemporalErrorsWithChildWorkflowFailureTest() {
    final Throwable cause = ApplicationFailure.newNonRetryableFailure("Test exception", "TestType");
    final Throwable throwable =
        new ChildWorkflowFailure(
            1,
            2,
            "workflowType",
            mock(WorkflowExecution.class),
            "default",
            RETRY_STATE_IN_PROGRESS,
            cause);
    final List<String> result = TemporalUtils.extractTemporalErrors(throwable);

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo("TestType: Test exception");
  }

  @Test
  void extractTemporalErrorsWithActivityFailureTest() {
    final Throwable cause = ApplicationFailure.newNonRetryableFailure("Test exception", "TestType");
    Throwable throwable =
        new ActivityFailure(
            "msg", 1, 2, "activityType", "activityId", RETRY_STATE_IN_PROGRESS, "identity", cause);
    List<String> result = TemporalUtils.extractTemporalErrors(throwable);

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo("TestType: Test exception");

    final Throwable cause2 =
        new ActivityFailure(
            "msg", 1, 2, "activityType", "activityId", RETRY_STATE_IN_PROGRESS, "identity", cause);

    throwable =
        new ChildWorkflowFailure(
            1,
            2,
            "workflowType",
            mock(WorkflowExecution.class),
            "default",
            RETRY_STATE_IN_PROGRESS,
            cause2);
    result = TemporalUtils.extractTemporalErrors(throwable);

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo("TestType: Test exception");

    throwable =
        new WorkflowFailedException(
            mock(WorkflowExecution.class),
            "workflowType",
            EventType.EVENT_TYPE_CHILD_WORKFLOW_EXECUTION_FAILED,
            1,
            RETRY_STATE_IN_PROGRESS,
            throwable);
    result = TemporalUtils.extractTemporalErrors(throwable);

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo("TestType: Test exception");

    throwable =
        new ActivityFailure(
            "msg", 1, 2, "activityType", "activityId", RETRY_STATE_IN_PROGRESS, "identity", cause);
    result = TemporalUtils.extractTemporalErrors(throwable);

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo("TestType: Test exception");
  }
}
