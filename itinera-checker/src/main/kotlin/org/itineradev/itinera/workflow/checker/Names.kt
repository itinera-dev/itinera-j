package org.itineradev.itinera.workflow.checker

/** The binary names of what itinera's API declares, which the checker reads by name. */
internal object Names {
    private const val ITINERA = "org.itineradev.itinera"

    const val WORKFLOW = "$ITINERA.workflow.Workflow"
    const val WITH_STEP = "$ITINERA.workflow.WithStep"
    const val INPUT_ADAPTER = "$ITINERA.workflow.InputAdapter"
    const val ID_GENERATOR = "$ITINERA.workflow.IdGenerator"
    const val KEY = "$ITINERA.workflow.Key"
    const val ITINERA_WORKFLOW = "$ITINERA.workflow.ItineraWorkflow"
    const val ITINERA_ASYNC_WORKFLOW = "$ITINERA.workflow.async.ItineraAsyncWorkflow"
    const val SUPPLIED = "$ITINERA.workflow.Supplied"

    const val STEP = "$ITINERA.step.Step"
    const val RESCUE_STEP = "$ITINERA.step.RescueStep"
    const val ITINERA_RESCUE_STEP = "$ITINERA.step.ItineraRescueStep"
    const val INPUT = "$ITINERA.step.Input"
    const val FROM_RESCUED_STEP = "$ITINERA.step.FromRescuedStep"
    const val OUTCOME = "$ITINERA.step.Outcome"
    const val RESCUE_OUTCOME = "$ITINERA.step.RescueOutcome"
    const val STEP_NAME = "$ITINERA.step.StepName"
    const val ATTEMPT = "$ITINERA.step.Attempt"
    const val REASON = "$ITINERA.step.Reason"
    const val STEP_REPORTER = "$ITINERA.step.StepReporter"
    const val ASYNC_STEP_REPORTER = "$ITINERA.step.async.AsyncStepReporter"

    const val STEP_POLICY = "$ITINERA.policy.StepPolicy"
    const val WORKFLOW_POLICY = "$ITINERA.policy.WorkflowPolicy"
    const val FROM_STEP = "$ITINERA.policy.FromStep"
    const val FROM_WORKFLOW = "$ITINERA.policy.FromWorkflow"
    const val ROLE = "$ITINERA.policy.Role"
    const val ON_SUCCESS = "$ITINERA.policy.OnSuccess"
    const val ON_FAILURE = "$ITINERA.policy.OnFailure"
    const val RETRY_CAUSE = "$ITINERA.policy.RetryCause"
    const val HOOK_REPORTER = "$ITINERA.policy.HookReporter"
    const val ASYNC_HOOK_REPORTER = "$ITINERA.policy.async.AsyncHookReporter"

    const val SERVICE = "$ITINERA.service.Service"
    const val PROVIDER = "$ITINERA.service.Provider"
    const val PROVIDED = "$ITINERA.service.Provided"
    const val PROVIDER_REPORTER = "$ITINERA.service.ProviderReporter"
    const val ASYNC_PROVIDER_REPORTER = "$ITINERA.service.async.AsyncProviderReporter"

    const val CONTRIBUTOR = "$ITINERA.journey.Contributor"
    const val JOURNEY_ID = "$ITINERA.journey.JourneyId"
    const val DATA_BAG = "$ITINERA.journey.DataBag"
    const val DATA_BAG_ACCESS = "$ITINERA.journey.DataBagAccess"
    const val FAILURE_CAUSE = "$ITINERA.journey.FailureCause"

    const val REPORTER = "$ITINERA.report.Reporter"
    const val ASYNC_REPORTER = "$ITINERA.report.async.AsyncReporter"

    const val OPTIONAL = "java.util.Optional"
    const val STRING = "java.lang.String"
    const val OBJECT = "java.lang.Object"
    const val THROWABLE = "java.lang.Throwable"
    const val COLLECTION = "java.util.Collection"
    const val MAP = "java.util.Map"
    const val ZONE_ID = "java.time.ZoneId"
    const val UNIT = "kotlin.Unit"
    const val METADATA = "kotlin.Metadata"

    const val NULLABLE = "org.jspecify.annotations.Nullable"
    const val NON_NULL = "org.jspecify.annotations.NonNull"
    const val NULL_MARKED = "org.jspecify.annotations.NullMarked"
    const val NULL_UNMARKED = "org.jspecify.annotations.NullUnmarked"
}
