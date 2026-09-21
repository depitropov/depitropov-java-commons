package dev.depitropov.commons.temporal;

import static dev.depitropov.commons.MdcUtils.setMdcKey;
import static dev.depitropov.commons.temporal.MdcTemporalUtils.MDC_ACTIVITY_ID;
import static dev.depitropov.commons.temporal.MdcTemporalUtils.MDC_ACTIVITY_TYPE;
import static dev.depitropov.commons.temporal.MdcTemporalUtils.MDC_WORKFLOW_ID;
import static dev.depitropov.commons.temporal.MdcTemporalUtils.MDC_WORKFLOW_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * Guards the temporal-sdk behavior that MDC does not leak between activity executions on the same
 * pooled worker thread.
 *
 * <p>Since temporal-sdk 1.30.0, {@code io.temporal.internal.worker.PollTaskExecutor} clears the MDC
 * around every activity task (regular, eager-dispatched, and local activities), so identity keys
 * set by one activity (e.g. {@code user}/{@code companyId}) never bleed into the log lines and span
 * tags of the next activity scheduled on the same thread. Log/trace attribution during incidents
 * relies on this. If an SDK upgrade breaks this test, the fix is a worker interceptor that clears
 * the MDC around each activity execution.
 */
class TemporalActivityMdcIsolationTest {

  private static final String TASK_QUEUE = "mdc-isolation-test-task-queue";

  private static final String MDC_USER = "user";

  private static final String MDC_USER_VALUE = "alice";

  private TestWorkflowEnvironment testEnv;

  private MdcCapturingActivitiesImpl activities;

  @BeforeEach
  void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();

    final Worker worker =
        testEnv.newWorker(
            TASK_QUEUE,
            WorkerOptions.newBuilder().setMaxConcurrentActivityExecutionSize(1).build());

    activities = new MdcCapturingActivitiesImpl();

    worker.registerWorkflowImplementationTypes(TwoActivitiesWorkflowImpl.class);
    worker.registerActivitiesImplementations(activities);

    testEnv.start();
  }

  @AfterEach
  void tearDown() {
    testEnv.close();
  }

  @Test
  void shouldNotLeakIdentityMdcKeyBetweenActivitiesOnSameThread() {
    runWorkflow();

    assertThat(activities.captureThreadId)
        .as("both activities must run on the same executor thread for the test to be meaningful")
        .isEqualTo(activities.setIdentityThreadId);

    assertThat(activities.mdcAtCaptureEntry)
        .as("identity key set by the previous activity must not leak into the next activity")
        .doesNotContainKey(MDC_USER);
  }

  @Test
  void shouldKeepOwnActivityMdcKeysSetInActivity() {
    runWorkflow();

    assertThat(activities.mdcAfterSetMdcTemporalActivity)
        .as("SDK MDC clearing must not interfere with the activity's own MDC keys")
        .containsKeys(MDC_WORKFLOW_TYPE, MDC_WORKFLOW_ID, MDC_ACTIVITY_TYPE, MDC_ACTIVITY_ID);
  }

  private void runWorkflow() {
    final TwoActivitiesWorkflow workflow =
        testEnv
            .getWorkflowClient()
            .newWorkflowStub(
                TwoActivitiesWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());

    workflow.run();
  }

  @WorkflowInterface
  public interface TwoActivitiesWorkflow {

    @WorkflowMethod
    void run();
  }

  public static class TwoActivitiesWorkflowImpl implements TwoActivitiesWorkflow {

    private final MdcCapturingActivities activities =
        Workflow.newActivityStub(
            MdcCapturingActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    public void run() {
      activities.setIdentityMdcKey();
      activities.captureMdc();
    }
  }

  @ActivityInterface
  public interface MdcCapturingActivities {

    void setIdentityMdcKey();

    void captureMdc();
  }

  public static class MdcCapturingActivitiesImpl implements MdcCapturingActivities {

    private volatile long setIdentityThreadId;

    private volatile long captureThreadId;

    private volatile Map<String, String> mdcAtCaptureEntry;

    private volatile Map<String, String> mdcAfterSetMdcTemporalActivity;

    @Override
    public void setIdentityMdcKey() {
      setIdentityThreadId = Thread.currentThread().getId();

      setMdcKey(MDC_USER, MDC_USER_VALUE);
    }

    @Override
    public void captureMdc() {
      captureThreadId = Thread.currentThread().getId();

      mdcAtCaptureEntry = copyOfMdc();

      MdcTemporalUtils.setMdcTemporalActivity();

      mdcAfterSetMdcTemporalActivity = copyOfMdc();
    }

    private static Map<String, String> copyOfMdc() {
      final Map<String, String> mdc = MDC.getCopyOfContextMap();

      return mdc != null ? mdc : Map.of();
    }
  }
}
