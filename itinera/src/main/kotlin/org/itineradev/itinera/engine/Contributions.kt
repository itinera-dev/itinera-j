package org.itineradev.itinera.engine

import org.itineradev.itinera.event.NotAValue as NotAValueAbort
import org.itineradev.itinera.event.ValueSource
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.step.StepName

/**
 * The contributor of one attempt of a step, or of one call of a hook, in [journey], which [owner]
 * names: it keeps each value contributed, captured when it is contributed, the last one under a key
 * winning, until the attempt or the call ends and closes it. A contribution that is not a value
 * aborts the journey during [step], saying [by] gave it.
 */
internal class Contributions(
    private val journey: Journey,
    private val step: StepName?,
    private val by: ValueSource,
    private val owner: String,
) : Contributor {
    private val kept = LinkedHashMap<String, Captured>()
    private var open = true

    /**
     * Keeps a captured copy of [value] under [key].
     *
     * @throws IllegalStateException once the attempt or the call has ended.
     * @throws JourneyAbortSignal when [value] is not a value, and once the journey is aborted.
     */
    @Synchronized
    override fun contribute(key: String, value: Any?) {
        check(open) { "$owner has ended, so its contributor contributes nothing." }
        journey.ensureNotAborted()
        when (val capture = Values.capture(value)) {
            is Captured -> kept[key] = capture
            is NotAValue -> throw notAValue(key, capture.error)
        }
    }

    /** What it kept, under each key in the order the keys were first given. */
    val contributed: Map<String, Captured>
        @Synchronized get() = kept.toMap()

    /** Closes the contributor, once its attempt or call has ended. */
    @Synchronized
    fun close() {
        open = false
    }

    private fun notAValue(key: String, error: Throwable?): JourneyAbortSignal =
        journey.abort(Abort(NotAValueAbort.UnderKey(step, by, key, error?.let(::messageOf)), error))
}
