package org.itineradev.itinera.report.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.report.async.AsyncReporter

/**
 * A reporter for tests of asynchronous workflows, which needs `itinera-kotlin`: it keeps every
 * event it receives, in order, and the latest value of each entry of global metadata, for the test
 * to read once the journey is over, exactly as [RecordingReporter] does for synchronous workflows.
 * It never fails a journey.
 *
 * @sample org.itineradev.itinera.report.testing.samples.asyncRecording
 */
@ExperimentalItineraApi
public class AsyncRecordingReporter : AsyncReporter {
    private val recording = Recording()

    /**
     * Every event received so far, in the order received. Each read is a new list.
     *
     * @sample org.itineradev.itinera.report.testing.samples.asyncRecording
     */
    public val events: List<Event>
        get() = recording.events()

    /**
     * The global metadata received so far: each key with its latest value, in the order the keys
     * were first set, so the journey ID comes first. Each read is a new map.
     *
     * @sample org.itineradev.itinera.report.testing.samples.asyncRecording
     */
    public val globalMetadata: Map<String, Any?>
        get() = recording.globalMetadata()

    /**
     * The journey's path, one line for each event of its flow, written as [RecordingReporter.path]
     * says.
     *
     * @sample org.itineradev.itinera.report.testing.samples.asyncRecording
     */
    public val path: List<String>
        get() = recording.path()

    /**
     * Keeps the event.
     *
     * @sample org.itineradev.itinera.report.testing.samples.asyncRecording
     */
    @JvmSynthetic
    override suspend fun report(event: Event) {
        recording.report(event)
    }

    /**
     * Keeps the entry, replacing the value of a key set before.
     *
     * @sample org.itineradev.itinera.report.testing.samples.asyncRecording
     */
    @JvmSynthetic
    override suspend fun setGlobalMetadata(key: String, value: Any?) {
        recording.setGlobalMetadata(key, value)
    }
}
