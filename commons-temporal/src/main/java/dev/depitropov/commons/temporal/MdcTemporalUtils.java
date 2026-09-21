package dev.depitropov.commons.temporal;

import static dev.depitropov.commons.MdcUtils.setMdcKey;

import io.temporal.activity.Activity;
import io.temporal.workflow.Workflow;

public final class MdcTemporalUtils {

  public static final String MDC_WORKFLOW_TYPE = "workflowType";

  public static final String MDC_WORKFLOW_ID = "workflowId";

  public static final String MDC_RUN_ID = "runId";

  public static final String MDC_ACTIVITY_TYPE = "activityType";

  public static final String MDC_ACTIVITY_ID = "activityId";

  public static void setMdcTemporalWorkflow() {
    setMdcKey(MDC_WORKFLOW_TYPE, Workflow.getInfo().getWorkflowType());
    setMdcKey(MDC_WORKFLOW_ID, Workflow.getInfo().getWorkflowId());
    setMdcKey(MDC_RUN_ID, Workflow.getInfo().getRunId());
  }

  public static void setMdcTemporalActivity() {
    setMdcKey(MDC_WORKFLOW_TYPE, Activity.getExecutionContext().getInfo().getWorkflowType());
    setMdcKey(MDC_WORKFLOW_ID, Activity.getExecutionContext().getInfo().getWorkflowId());
    setMdcKey(MDC_RUN_ID, Activity.getExecutionContext().getInfo().getRunId());

    setMdcKey(MDC_ACTIVITY_TYPE, Activity.getExecutionContext().getInfo().getActivityType());
    setMdcKey(MDC_ACTIVITY_ID, Activity.getExecutionContext().getInfo().getActivityId());
  }
}
