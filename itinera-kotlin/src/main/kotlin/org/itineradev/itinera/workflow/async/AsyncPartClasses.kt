package org.itineradev.itinera.workflow.async

import kotlin.reflect.KClass
import org.itineradev.itinera.step.ItineraRescueStep
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepBuilder
import org.itineradev.itinera.workflow.Violations

/**
 * Starts the descriptor of an asynchronous step written as a class marked `@Step`, built for each
 * attempt with its constructor and run with its `suspend` method `run`, which request what the step
 * requests. The builder gives the rest, as a workflow class's `@WithStep` does: a retry budget of 0
 * and abnormal terminations retried, unless it says otherwise, and the step's policies and rescue
 * step.
 *
 * @throws IllegalArgumentException when [name] is empty.
 * @throws Violations with every mistake in the class's declaration.
 * @sample org.itineradev.itinera.workflow.async.samples.asyncDescribingParts
 */
public fun asyncStep(name: String, type: KClass<out ItineraStep>): StepBuilder<Async> =
    asyncPartClasses.step(StepName.of(name), type.java)

/**
 * The descriptor of an asynchronous rescue step written as a class marked `@RescueStep`, built with
 * its constructor and run with its `suspend` method `run`, which request what the rescue step
 * requests.
 *
 * @throws Violations with every mistake in the class's declaration.
 * @sample org.itineradev.itinera.workflow.async.samples.asyncDescribingParts
 */
public fun asyncRescueStepOf(type: KClass<out ItineraRescueStep>): RescueStepDescriptor<Async> =
    asyncPartClasses.rescue(type.java)
