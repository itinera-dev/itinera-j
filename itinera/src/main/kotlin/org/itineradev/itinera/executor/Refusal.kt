package org.itineradev.itinera.executor

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.workflow.Violation

/**
 * What `run` throws when it refuses an instance, before any journey and with no event: [refused]
 * says why, and the error that caused it, when custom code failed, is also its [cause].
 *
 * Java sees it as checked, since `run` declares it.
 *
 * @sample org.itineradev.itinera.executor.samples.refusals
 */
@ExperimentalItineraApi
public class Refusal(
    /** Why `run` refused the instance. */
    public val refused: Refused
) : Exception(refused.message, refused.error)

/**
 * Why `run` refused an instance, before any journey and with no event.
 *
 * @sample org.itineradev.itinera.executor.samples.refusals
 */
@ExperimentalItineraApi
public sealed interface Refused {
    /** What the refusal says. */
    public val message: String

    /** What the custom code threw, when its failure caused the refusal. */
    public val error: Throwable?

    /**
     * The instance was handed to an executor before: an instance runs at most one journey, so each
     * journey needs an instance of its own.
     *
     * @sample org.itineradev.itinera.executor.samples.refusals
     */
    public data object InstanceRan : Refused {
        override val message: String
            get() =
                "run refused an instance handed to an executor before: create an instance for each journey"

        override val error: Throwable?
            get() = null
    }

    /**
     * The executor was running a journey: it runs one at a time, and parallelism comes from several
     * executors.
     *
     * @sample org.itineradev.itinera.executor.samples.refusals
     */
    public data object JourneyRunning : Refused {
        override val message: String
            get() =
                "run refused an instance while the executor runs a journey: wait for it, or run on another executor"

        override val error: Throwable?
            get() = null
    }

    /**
     * The instance is not admitted, for every one of these [violations]: its workflow or one of its
     * parts is in a mode the executor does not accept, its initial data or global metadata holds
     * what is not a value or sets `journey_id`, or a descriptor that the builder did not make
     * breaks a rule.
     *
     * @sample org.itineradev.itinera.executor.samples.refusals
     */
    @JvmRecord
    public data class Inadmissible(
        /** Every violation found, in the order found. */
        val violations: List<Violation>
    ) : Refused {
        override val message: String
            get() = violations.joinToString("; ", "run refused an instance: ")

        override val error: Throwable?
            get() = null
    }

    /**
     * The executor's dispatcher factory threw while it made the journey's dispatcher.
     *
     * @sample org.itineradev.itinera.executor.samples.refusals
     */
    @JvmRecord
    public data class DispatcherFactoryFailed(
        /** What the factory threw. */
        override val error: Throwable,
        /** Its message, or a fixed text when it has none. */
        val errorMessage: String,
    ) : Refused {
        override val message: String
            get() = "run refused an instance: the dispatcher factory failed: $errorMessage"
    }

    /**
     * The journey's dispatcher threw while the instance's reporters were added to it.
     *
     * @sample org.itineradev.itinera.executor.samples.refusals
     */
    @JvmRecord
    public data class DispatcherFailed(
        /** What the dispatcher threw. */
        override val error: Throwable,
        /** Its message, or a fixed text when it has none. */
        val errorMessage: String,
    ) : Refused {
        override val message: String
            get() =
                "run refused an instance: the dispatcher failed while the reporters were added: $errorMessage"
    }

    /**
     * A reporter, or the dispatcher, threw while the journey ID or the initial global metadata was
     * passed to the reporters.
     *
     * @sample org.itineradev.itinera.executor.samples.refusals
     */
    @JvmRecord
    public data class GlobalMetadataNotSet(
        /** What the reporter or the dispatcher threw. */
        override val error: Throwable,
        /** Its message, or a fixed text when it has none. */
        val errorMessage: String,
    ) : Refused {
        override val message: String
            get() = "run refused an instance: the global metadata could not be set: $errorMessage"
    }

    /**
     * The executor's dispatcher factory returned `null`, which only Java code can, and which is not
     * a failure of that code, so the refusal carries no error.
     *
     * @sample org.itineradev.itinera.executor.samples.refusals
     */
    public data object DispatcherFactoryReturnedNull : Refused {
        override val message: String
            get() =
                "run refused an instance: the dispatcher factory returned null, not a dispatcher"

        override val error: Throwable?
            get() = null
    }
}
