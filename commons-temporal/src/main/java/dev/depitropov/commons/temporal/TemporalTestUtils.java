package dev.depitropov.commons.temporal;

import static dev.depitropov.commons.temporal.TemporalUtils.SEARCH_ATTR_KEY_COMPANY_ID;
import static dev.depitropov.commons.temporal.TemporalUtils.SEARCH_ATTR_KEY_USER_ID;
import static io.temporal.api.enums.v1.IndexedValueType.INDEXED_VALUE_TYPE_TEXT;
import static java.util.Objects.requireNonNull;

import io.temporal.api.operatorservice.v1.AddSearchAttributesRequest;
import io.temporal.api.operatorservice.v1.AddSearchAttributesResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.OperatorServiceStubs;
import io.temporal.serviceclient.OperatorServiceStubsOptions;
import java.util.List;

public final class TemporalTestUtils {

  public static void configureTemporalSearchAttributes(final WorkflowClient workflowClient) {
    configureTemporalSearchAttributes(
        workflowClient, List.of(SEARCH_ATTR_KEY_COMPANY_ID, SEARCH_ATTR_KEY_USER_ID));
  }

  public static void configureTemporalSearchAttributes(
      final WorkflowClient workflowClient, final List<String> textKeys) {
    try {
      final AddSearchAttributesRequest.Builder builder =
          AddSearchAttributesRequest.newBuilder()
              .setNamespace(workflowClient.getOptions().getNamespace());

      for (final String key : textKeys) {
        builder.putSearchAttributes(key, INDEXED_VALUE_TYPE_TEXT);
      }

      final AddSearchAttributesRequest request = builder.build();

      final OperatorServiceStubs operatorServiceStubs =
          OperatorServiceStubs.newServiceStubs(
              OperatorServiceStubsOptions.newBuilder()
                  .setChannel(workflowClient.getWorkflowServiceStubs().getRawChannel())
                  .validateAndBuildWithDefaults());

      final AddSearchAttributesResponse res =
          operatorServiceStubs.blockingStub().addSearchAttributes(request);

      requireNonNull(res);

    } catch (final Exception e) {

      if (!e.getMessage().contains("ALREADY_EXISTS: Search attribute")) {
        throw e;
      }
    }
  }
}
