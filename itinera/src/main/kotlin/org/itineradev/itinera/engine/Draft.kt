package org.itineradev.itinera.engine

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.report.ReporterFactoryOf
import org.itineradev.itinera.report.ReporterOf
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.WorkflowDescriptor

/**
 * What an instance builder of either mode has been given, until it creates the instance.
 *
 * Part of itinera's engine, not of its API.
 */
public class Draft<W, M : Mode>(
    private val descriptor: WorkflowDescriptor<W, M>,
    private val workflow: W,
) {
    private val data = LinkedHashMap<String, Any?>()
    private val metadata = LinkedHashMap<String, Any?>()
    private val reporters = mutableListOf<(JourneyId) -> ReporterOf<M>?>()

    /** Puts [value] in the initial data under [key]. */
    public fun data(key: String, value: Any?) {
        data[key] = value
    }

    /**
     * Puts each component of [value], a record or data class, in the initial data under its key.
     *
     * @throws IllegalArgumentException when [value] is neither, or as `JourneyData.of` says.
     */
    public fun initialData(value: Any) {
        data.putAll(JourneyData.of(ClassType(value.javaClass, emptyList(), false)).entries(value))
    }

    /** Sets [value] as the initial global metadata under [key]. */
    public fun metadata(key: String, value: Any?) {
        metadata[key] = value
    }

    /** Adds the reporter [factory] makes for the journey's ID. */
    public fun reporter(factory: (JourneyId) -> ReporterOf<M>?) {
        reporters += factory
    }

    /**
     * The instance: captures the initial data, then produces the journey ID, then makes the
     * reporters, those the workflow lists first.
     *
     * @throws IllegalStateException when the ID generator or a reporter factory throws or returns
     *   what it may not.
     */
    public fun create(): WorkflowInstance<M> {
        val initial = Initial.of(data, metadata)
        val journeyId = journeyId(initial)
        return Created(
            descriptor,
            workflow,
            journeyId,
            factories().map { factory -> made(factory, journeyId) },
            data.toMap(),
            metadata.toMap(),
            initial,
        )
    }

    private fun factories(): List<(JourneyId) -> ReporterOf<M>?> =
        descriptor.reporters.map(::making) + reporters

    private fun journeyId(initial: Initial): JourneyId {
        val generator = descriptor.idGenerator ?: return JourneyId.of(UUID.randomUUID().toString())
        val bag = DataBags.of(initial.data.values)
        val id = runCatching {
            nullable(generator.generate(workflow, bag))
        }
            .onFailure(::rethrowError)
            .getOrElse { thrown -> throw failed("The ID generator", thrown) }
        check(!id.isNullOrEmpty()) {
            "The ID generator returned ${if (id == null) "null" else "an empty string"}: a journey ID is never empty."
        }
        return JourneyId.of(id)
    }
}

private fun <M : Mode> making(factory: ReporterFactoryOf<M>): (JourneyId) -> ReporterOf<M>? =
    factory::create

private fun <M : Mode> made(
    factory: (JourneyId) -> ReporterOf<M>?,
    journeyId: JourneyId,
): ReporterOf<M> =
    runCatching { factory(journeyId) }
        .onFailure(::rethrowError)
        .getOrElse { thrown -> throw failed("A reporter factory", thrown) }
        ?: throw IllegalStateException("A reporter factory returned null, not a reporter.")

private fun failed(what: String, thrown: Throwable): IllegalStateException =
    IllegalStateException(
        "$what threw while the instance was created: ${messageOf(thrown)}",
        thrown,
    )

/** An instance an instance builder created, with its initial values captured. */
internal class Created<M : Mode>(
    override val descriptor: WorkflowDescriptor<*, M>,
    override val workflow: Any?,
    override val journeyId: JourneyId,
    override val reporters: List<ReporterOf<M>>,
    override val initialData: Map<String, Any?>,
    override val initialGlobalMetadata: Map<String, Any?>,
    val initial: Initial,
) : WorkflowInstance<M> {
    private val claimed = AtomicBoolean()

    override fun claim(): Boolean = claimed.compareAndSet(false, true)
}
