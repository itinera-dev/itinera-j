package org.itineradev.itinera.engine

import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.NotAValue as NotAValueAbort
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.step.StepName

/**
 * The contributor of one attempt of [step] in [journey]: it keeps each value contributed, captured
 * when it is contributed, the last one under a key winning, until the attempt ends and closes it.
 */
internal class Contributions(private val journey: Journey, private val step: StepName) :
    Contributor {
    private val kept = LinkedHashMap<String, Captured>()
    private var open = true

    /**
     * Keeps a captured copy of [value] under [key].
     *
     * @throws IllegalStateException once the attempt has ended.
     * @throws JourneyAbortSignal when [value] is not a value, and once the journey is aborted.
     */
    @Synchronized
    override fun contribute(key: String, value: Any?) {
        check(open) { "The attempt of $step has ended, so its contributor contributes nothing." }
        journey.ensureNotAborted()
        when (val capture = Values.capture(value)) {
            is Captured -> kept[key] = capture
            is NotAValue -> throw notAValue(key, capture.error)
        }
    }

    /** What it kept, under each key in the order the keys were first given. */
    val contributed: Map<String, Captured>
        @Synchronized get() = kept.toMap()

    /** Closes the contributor, once its attempt has ended. */
    @Synchronized
    fun close() {
        open = false
    }

    private fun notAValue(key: String, error: Throwable?): JourneyAbortSignal =
        journey.abort(
            Abort(NotAValueAbort.UnderKey(step, ByStep, key, error?.let(::messageOf)), error)
        )
}
