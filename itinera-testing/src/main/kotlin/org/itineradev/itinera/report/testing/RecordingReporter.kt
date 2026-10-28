package org.itineradev.itinera.report.testing

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.event.Event
import org.itineradev.itinera.report.Reporter

/**
 * A reporter for tests of synchronous workflows: it keeps every event it receives, in order, and
 * the latest value of each entry of global metadata, for the test to read once the journey is over.
 * It never fails a journey. [AsyncRecordingReporter] is its twin for asynchronous workflows.
 *
 * @sample org.itineradev.itinera.report.testing.samples.recording
 */
@ExperimentalItineraApi
public class RecordingReporter : Reporter {
    private val recording = Recording()

    /**
     * Every event received so far, in the order received. Each read is a new list.
     *
     * @sample org.itineradev.itinera.report.testing.samples.recording
     */
    public val events: List<Event>
        get() = recording.events()

    /**
     * The global metadata received so far: each key with its latest value, in the order the keys
     * were first set, so the journey ID comes first. Each read is a new map.
     *
     * @sample org.itineradev.itinera.report.testing.samples.metadata
     */
    public val globalMetadata: Map<String, Any?>
        get() = recording.globalMetadata()

    /**
     * The journey's path: one line for each event of its flow, in the order received, so a test can
     * compare it with the path it expects. Only the flow has lines: the outcome of each attempt,
     * each decision on a step, and how the journey ended; inputs, services, hooks, contributions,
     * emitted events and global metadata have none, and no line holds a timestamp, a sequence
     * number or an ID, so a path changes only when the flow does. The lines read:
     * - `charge attempt 1 succeeded`; `charge attempt 1 failed: declined`, with the reason's code;
     *   `charge attempt 1 skipped`, followed by `: ` and the reason's code when it has one; and
     *   `charge attempt 1 terminated abnormally: timeout`, with the error's message. A rescue
     *   step's attempts read the same under its own name, such as `charge_rescue`.
     * - `charge retrying`; `charge given up: retries exhausted`, or `failure` or `abnormal
     *   termination`, or `FailWorkflow from fraud_check` when a hook of the policy `fraud_check`
     *   gave it up.
     * - `charge rescuing with charge_rescue` and `charge rescued by charge_rescue`.
     * - `journey succeeded`, or `journey succeeded, decided by early_exit` when a hook of the
     *   policy `early_exit` decided it; `journey failed at charge: declined`, with the reason's
     *   code, or the error's message when the journey failed with no reason; `journey aborted at
     *   charge: step could not be built`, or `journey aborted: reporter failed` when the abort
     *   concerns no step.
     *
     * A refused journey has no events, so its path is empty.
     *
     * @sample org.itineradev.itinera.report.testing.samples.path
     */
    public val path: List<String>
        get() = recording.path()

    /**
     * Keeps the event.
     *
     * @sample org.itineradev.itinera.report.testing.samples.recording
     */
    override fun report(event: Event) {
        recording.report(event)
    }

    /**
     * Keeps the entry, replacing the value of a key set before.
     *
     * @sample org.itineradev.itinera.report.testing.samples.metadata
     */
    override fun setGlobalMetadata(key: String, value: Any?) {
        recording.setGlobalMetadata(key, value)
    }
}
