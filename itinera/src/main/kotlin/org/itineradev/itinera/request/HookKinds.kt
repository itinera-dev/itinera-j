package org.itineradev.itinera.request

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.workflow.Mode

/**
 * A hook of a policy of a workflow of mode [M], as the kind of part that requests. Every hook may
 * request data from the workflow, the journey ID, a contributor, the hook reporter of its mode and
 * roles.
 *
 * @param M the mode of the workflows the hook can join.
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi
public interface HookKind<M : Mode> :
    WorkflowDataKind, JourneyIdKind, ContributorKind, HookReporterKind<M>, RoleKind

/**
 * A workflow hook, `on workflow success` or `on workflow failure`, of a workflow of mode [M]: what
 * every hook may request, and nothing more.
 *
 * @param M the mode of the workflows the hook can join.
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi public interface WorkflowHookKind<M : Mode> : HookKind<M>

/**
 * A step hook of a workflow of mode [M], as `on step success` and `on step abnormal termination`
 * are: what every hook may request, and, from the step it acts on, data it contributed, its name,
 * the attempt, the failure's reason and the error.
 *
 * @param M the mode of the workflows the hook can join.
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi
public interface StepHookKind<M : Mode> :
    HookKind<M>, FromStepKind, StepNameKind, AttemptKind, ReasonKind, ErrorKind

/**
 * `on step failure` of a workflow of mode [M]: what any step hook may request, and the failure's
 * cause.
 *
 * @param M the mode of the workflows the hook can join.
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi
public interface StepFailureKind<M : Mode> : StepHookKind<M>, FailureCauseKind

/**
 * `on step retry` of a workflow of mode [M]: what any step hook may request, and the retry's cause.
 *
 * @param M the mode of the workflows the hook can join.
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi public interface StepRetryKind<M : Mode> : StepHookKind<M>, RetryCauseKind

/**
 * The kinds of part that may request data from the workflow, read from the data bag and never
 * through an input adapter: hooks.
 *
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi public interface WorkflowDataKind

/**
 * The kinds of part that may request the journey ID: hooks.
 *
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi public interface JourneyIdKind

/**
 * The kinds of part of mode [M] that may request the hook reporter of that mode: hooks.
 *
 * @param M the mode of the reporter they may request.
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi public interface HookReporterKind<M : Mode>

/**
 * The kinds of part that may request roles: hooks.
 *
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi public interface RoleKind

/**
 * The kinds of part that may request data a step contributed: step hooks, from the step they act
 * on.
 *
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi public interface FromStepKind

/**
 * The kinds of part that may request the name of the step they act on: step hooks.
 *
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi public interface StepNameKind

/**
 * The kinds of part that may request the attempt they act on: step hooks.
 *
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi public interface AttemptKind

/**
 * The kinds of part that may request the reason a step's attempt reported for failing: step hooks.
 *
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi public interface ReasonKind

/**
 * The kinds of part that may request the error that ended a step's attempt abnormally: step hooks.
 *
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi public interface ErrorKind

/**
 * The kinds of part that may request why a step failed: `on step failure`.
 *
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi public interface FailureCauseKind

/**
 * The kinds of part that may request why a step is retried: `on step retry`.
 *
 * @sample org.itineradev.itinera.request.samples.hookRequests
 */
@ExperimentalItineraApi public interface RetryCauseKind
