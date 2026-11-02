package org.itineradev.itinera.engine

import org.itineradev.itinera.event.Event
import org.itineradev.itinera.event.EventKind
import org.itineradev.itinera.event.MetadataSource
import org.itineradev.itinera.event.NotAValue as NotAValueAbort
import org.itineradev.itinera.event.ValueSource
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.step.StepName

/**
 * What a reporter handle given to custom code during [step], if any, emits through and sets global
 * metadata through in [journey], for [giver], whose global metadata [source] names, until what it
 * was made for ends and closes it. Each kind of handle says which events it emits.
 */
internal abstract class Emitting(
    private val journey: Journey,
    private val step: StepName?,
    private val giver: ValueSource,
    private val source: MetadataSource,
) : Emits {
    @Volatile private var open = true

    /** Why it reports nothing, once closed. */
    protected abstract val closed: String

    /** The event of [severity] it emits with [message] and [data], stamped as [stamp] says. */
    protected abstract fun eventOf(
        stamp: Stamp,
        severity: Severity,
        message: String,
        data: Any?,
    ): Event

    /** The kind of the event of [severity] it emits. */
    protected abstract fun kindOf(severity: Severity): EventKind

    /**
     * @throws JourneyAbortSignal when [data] is not a value, when a reporter fails on the event,
     *   and once the journey is aborted.
     */
    @JvmSynthetic
    final override suspend fun emit(severity: Severity, message: String, data: Any?) {
        ensureOpen()
        when (val capture = Values.capture(data)) {
            is Captured -> journey.emit { eventOf(this, severity, message, capture.value) }
            is NotAValue -> throw notAValue(severity, capture.error)
        }
    }

    /**
     * @throws JourneyAbortSignal when [key] is reserved, when [value] is not a value, when a
     *   reporter fails on the entry, and once the journey is aborted.
     */
    @JvmSynthetic
    final override suspend fun setGlobalMetadata(key: String, value: Any?) {
        ensureOpen()
        journey.setGlobalMetadata(key, value, giver, source)
    }

    final override fun close() {
        open = false
    }

    private fun ensureOpen() {
        check(open) { closed }
        journey.ensureNotAborted()
    }

    private fun notAValue(severity: Severity, error: Throwable?): JourneyAbortSignal =
        journey.abort(
            Abort(
                NotAValueAbort.InEvent(step, giver, kindOf(severity), error?.let(::messageOf)),
                error,
            )
        )
}
