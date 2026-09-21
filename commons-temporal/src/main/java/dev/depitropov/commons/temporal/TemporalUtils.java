package dev.depitropov.commons.temporal;

import static io.temporal.api.enums.v1.WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.ObjectUtils.requireNonEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.commons.lang3.exception.ExceptionUtils.getThrowableList;

import com.google.protobuf.Timestamp;
import dev.depitropov.commons.errors.BusinessRuleException;
import dev.depitropov.commons.errors.GeneralErrorCode;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionResponse;
import io.temporal.api.workflowservice.v1.WorkflowServiceGrpc;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowException;
import io.temporal.common.SearchAttributeKey;
import io.temporal.common.SearchAttributes;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.ChildWorkflowFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apache.commons.lang3.tuple.Pair;

public final class TemporalUtils {

  public static final String SEARCH_ATTR_KEY_COMPANY_ID = "companyId";

  public static final String SEARCH_ATTR_KEY_USER_ID = "userId";

  public static final DefaultDataConverter DEFAULT_DATA_CONVERTER =
      DefaultDataConverter.newDefaultInstance();

  private static final Set<Integer> TRANSIENT_4XX_STATUSES = Set.of(408, 429);

  public static <T> T nonRetryableFailureCheck(final Supplier<T> d) {
    return nonRetryableFailureCheck(d, TemporalUtils::isNonRetryableFailure);
  }

  public static <T> T nonRetryableFailureCheck(
      final Supplier<T> d, final Predicate<Exception> isNonRetryableFailureFunc) {
    try {

      return d.get();

    } catch (final Exception e) {
      if (isNonRetryableFailureFunc.test(e)) {
        throw ApplicationFailure.newNonRetryableFailureWithCause(
            e.getMessage(), e.getClass().getSimpleName(), e);
      }

      throw e;
    }
  }

  /**
   * Classifies an exception as a non-retryable (terminal) failure for Temporal activities.
   *
   * <p>A {@link BusinessRuleException} is non-retryable when its error code maps to a terminal 4xx
   * HTTP status. Transient 4xx statuses — 408 Request Timeout and 429 Too Many Requests — are
   * carved out and treated as retryable, matching the web-client layer's own retry-on-429 behavior
   * in commons {@code RetryUtils}.
   *
   * @param e the exception to classify
   * @return true if the exception is a terminal failure that must not be retried
   */
  public static boolean isNonRetryableFailure(final Exception e) {

    if (e instanceof final BusinessRuleException bre) {
      return is4xxClientError(GeneralErrorCode.httpStatusFromValue(bre.getErrorCode().errorCode()));
    }

    return e instanceof IllegalArgumentException || e instanceof IllegalStateException;
  }

  private static boolean is4xxClientError(final int httpStatus) {
    return httpStatus >= 400 && httpStatus < 500 && !TRANSIENT_4XX_STATUSES.contains(httpStatus);
  }

  public static SearchAttributes buildSearchAttributes(String companyId, String userId) {
    companyId = trimToNull(companyId);
    requireNonEmpty(companyId, SEARCH_ATTR_KEY_COMPANY_ID);

    final SearchAttributes.Builder builder =
        SearchAttributes.newBuilder()
            .set(SearchAttributeKey.forText(SEARCH_ATTR_KEY_COMPANY_ID), companyId);

    userId = trimToNull(userId);
    if (isNotEmpty(userId)) {
      builder.set(SearchAttributeKey.forText(SEARCH_ATTR_KEY_USER_ID), userId);
    }

    return builder.build();
  }

  public static String getSearchAttributeCompanyId(
      final DescribeWorkflowExecutionResponse response) {
    return getSearchAttribute(response, SEARCH_ATTR_KEY_COMPANY_ID);
  }

  public static String getSearchAttributeUserId(final DescribeWorkflowExecutionResponse response) {
    return getSearchAttribute(response, SEARCH_ATTR_KEY_USER_ID);
  }

  public static String getSearchAttribute(
      final DescribeWorkflowExecutionResponse response, final String key) {
    return DEFAULT_DATA_CONVERTER.fromPayload(
        response.getWorkflowExecutionInfo().getSearchAttributes().getIndexedFieldsOrThrow(key),
        String.class,
        String.class);
  }

  public static WorkflowExecutionTime buildWorkflowExecutionTime(
      final WorkflowExecutionInfo workflowExecutionInfo) {

    final Instant startTime =
        workflowExecutionInfo.hasStartTime()
            ? convertToInstant(workflowExecutionInfo.getStartTime())
            : null;

    final Instant executionTime =
        workflowExecutionInfo.hasExecutionTime()
            ? convertToInstant(workflowExecutionInfo.getExecutionTime())
            : null;

    final Instant closeTime =
        workflowExecutionInfo.hasCloseTime()
            ? convertToInstant(workflowExecutionInfo.getCloseTime())
            : null;

    return WorkflowExecutionTime.builder()
        .startTime(startTime)
        .executionTime(executionTime)
        .closeTime(closeTime)
        .build();
  }

  public static Instant convertToInstant(final Timestamp timestamp) {
    return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
  }

  public static boolean isWorkflowRunning(
      final String workflowId,
      final WorkflowClient workflowClient,
      final WorkflowServiceStubs workflowServiceStubs) {

    final WorkflowServiceGrpc.WorkflowServiceBlockingStub stub =
        workflowServiceStubs.blockingStub();

    final DescribeWorkflowExecutionRequest request =
        DescribeWorkflowExecutionRequest.newBuilder()
            .setNamespace(workflowClient.getOptions().getNamespace())
            .setExecution(WorkflowExecution.newBuilder().setWorkflowId(workflowId))
            .build();

    try {
      final DescribeWorkflowExecutionResponse response = stub.describeWorkflowExecution(request);

      final WorkflowExecutionStatus status = response.getWorkflowExecutionInfo().getStatus();

      return status == WORKFLOW_EXECUTION_STATUS_RUNNING;
    } catch (final StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
        return false;
      } else {
        throw e;
      }
    }
  }

  public static String extractTemporalErrorsAsString(final Throwable throwable) {
    return String.join("; ", extractTemporalErrors(throwable));
  }

  public static List<String> extractTemporalErrors(final Throwable throwable) {

    final List<Throwable> throwables = getThrowableList(throwable);

    return throwables.stream()
        .map(TemporalUtils::extractTemporalError)
        .filter(Objects::nonNull)
        .distinct()
        .map(
            iter -> {
              if (isEmpty(iter.getKey())) {
                return iter.getValue();
              }

              return iter.getKey() + ": " + iter.getValue();
            })
        .toList();
  }

  private static Pair<String, String> extractTemporalError(final Throwable throwable) {
    if (throwable instanceof final ApplicationFailure applicationFailure) {
      final String type = applicationFailure.getType();

      if (applicationFailure
          .getType()
          .toLowerCase()
          .contains(BusinessRuleException.class.getSimpleName().toLowerCase())) {
        return Pair.of("", applicationFailure.getOriginalMessage());
      }

      return Pair.of(type, applicationFailure.getOriginalMessage());
    }

    if (throwable instanceof final WorkflowException workflowException
        && isTemporalCauseException(workflowException)) {
      return null;
    }

    if (throwable instanceof final ChildWorkflowFailure childWorkflowFailure
        && isTemporalCauseException(childWorkflowFailure)) {
      return null;
    }

    if (throwable instanceof final ActivityFailure activityFailure
        && isTemporalCauseException(activityFailure)) {
      return null;
    }

    return Pair.of(throwable.getClass().getSimpleName(), throwable.getMessage());
  }

  private static boolean isTemporalCauseException(final Throwable throwable) {
    return throwable.getCause() instanceof ActivityFailure
        || throwable.getCause() instanceof ChildWorkflowFailure
        || throwable.getCause() instanceof ApplicationFailure;
  }
}
