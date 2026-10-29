package org.itineradev.itinera.request

import org.itineradev.itinera.workflow.Mode

/**
 * A step of a workflow of mode [M], as the kind of part that requests: what its factory's [Got]
 * reads. A step may request inputs and services, a contributor, and the step reporter of its mode.
 *
 * Kinds are only types: nothing is ever of one. Each says what a kind of part may request, and a
 * [Request] names the kinds that may make it.
 *
 * @param M the mode of the workflows the step can join.
 * @sample org.itineradev.itinera.request.samples.requests
 */
public interface StepKind<M : Mode> : BuildKind, ContributorKind, StepReporterKind<M>

/**
 * A rescue step of a workflow of mode [M], as the kind of part that requests: what a step may
 * request, and, from the step it rescues, the data its failed attempt contributed, the failure's
 * reason and the error.
 *
 * @param M the mode of the workflows the rescue step can join.
 * @sample org.itineradev.itinera.workflow.samples.rescues
 */
public interface RescueKind<M : Mode> : StepKind<M>, FromStepKind, ReasonKind, ErrorKind

/**
 * The kinds of part built from the data and services they request, which may request inputs and
 * services: steps and rescue steps.
 *
 * @sample org.itineradev.itinera.request.samples.requests
 */
public interface BuildKind

/**
 * The kinds of part that contribute data, which may request a contributor: steps, rescue steps and
 * hooks.
 *
 * @sample org.itineradev.itinera.request.samples.requests
 */
public interface ContributorKind

/**
 * The kinds of part of mode [M] that may request the step reporter of that mode: steps and rescue
 * steps.
 *
 * @param M the mode of the reporter they may request.
 * @sample org.itineradev.itinera.request.samples.requests
 */
public interface StepReporterKind<M : Mode>
