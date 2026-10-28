package org.itineradev.itinera.workflow

import kotlin.reflect.KClass
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.policy.ItineraWorkflowPolicy
import org.itineradev.itinera.report.ReporterOf

/**
 * Declares an [ItineraWorkflow] or an `ItineraAsyncWorkflow` class as a workflow and names it;
 * every event of its journeys carries the name. Without it, the workflow is named after the class's
 * simple name, which obfuscators rename, so naming it is advised.
 *
 * @sample org.itineradev.itinera.workflow.annotated.samples.shipping
 */
@ExperimentalItineraApi
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@MustBeDocumented
public annotation class Workflow(
    /** The workflow's name, which is never empty. */
    val name: String,
    /**
     * The workflow's policies, each a class marked `@WorkflowPolicy`, in the order their hooks run.
     */
    val policies: Array<KClass<out ItineraWorkflowPolicy>> = [],
    /**
     * The workflow's reporters, each a class of the workflow's mode with a constructor that takes
     * nothing: every instance builds its own of each, in this order, before the reporters it is
     * given itself.
     */
    val reporters: Array<KClass<out ReporterOf<*>>> = [],
)
