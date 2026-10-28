package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.service.ProviderName
import org.itineradev.itinera.step.StepName

/**
 * Where a step stands among its workflow's steps, counted from 1: the first step to run is at
 * position 1. Positions compare by their numbers.
 *
 * @sample org.itineradev.itinera.workflow.samples.listing
 */
@ExperimentalItineraApi
public class Position
private constructor(
    /** The position's number, counted from 1. */
    public val number: Int
) : Comparable<Position> {
    /** Which of the two positions comes first: below 0 when this one does. */
    override fun compareTo(other: Position): Int = number.compareTo(other.number)

    /** Whether the other is a position of the same number. */
    override fun equals(other: Any?): Boolean = other is Position && other.number == number

    /** The number, which equal positions share. */
    override fun hashCode(): Int = number

    /** The number, in decimal. */
    override fun toString(): String = number.toString()

    /** Makes positions. */
    public companion object {
        /**
         * The position numbered [number].
         *
         * @throws IllegalArgumentException when [number] is below 1.
         * @sample org.itineradev.itinera.workflow.samples.listing
         */
        @JvmStatic
        public fun of(number: Int): Position {
            require(number >= 1) { "A position is counted from 1, but was given $number." }
            return Position(number)
        }
    }
}

/**
 * One step of a workflow, as [WorkflowDescriptor.listing] shows it, without running anything.
 *
 * @sample org.itineradev.itinera.workflow.samples.listing
 */
@ExperimentalItineraApi
@JvmRecord
public data class ListedStep(
    /** The step's name. */
    val step: StepName,
    /** Where the step stands among the workflow's steps. */
    val position: Position,
    /** The names of the step's policies, in the order they are attached. */
    val policies: List<PolicyName>,
    /** The name of the input adapter attached to the step, or `null` when none is. */
    val adapter: AdapterName?,
    /** The services the step requests, in the order it requests them. */
    val services: List<ListedService>,
    /** The step's rescue section, or `null` when the step has no rescue step. */
    val rescue: ListedRescue?,
)

/**
 * The rescue section of a listed step: its rescue step, which has no position, since it runs only
 * when its step fails, and no policies, since a rescue step has none.
 *
 * @sample org.itineradev.itinera.workflow.samples.listing
 */
@ExperimentalItineraApi
@JvmRecord
public data class ListedRescue(
    /** The rescue step's name, `<step name>_rescue`. */
    val step: StepName,
    /** The name of the input adapter attached to the rescue step, or `null` when none is. */
    val adapter: AdapterName?,
    /** The services the rescue step requests, in the order it requests them. */
    val services: List<ListedService>,
)

/**
 * A service a listed step or rescue step requests, with the provider that serves it.
 *
 * @sample org.itineradev.itinera.workflow.samples.listing
 */
@ExperimentalItineraApi
@JvmRecord
public data class ListedService(
    /** The key the service is requested by. */
    val key: String,
    /** The name of the service provider that serves it. */
    val provider: ProviderName,
)
