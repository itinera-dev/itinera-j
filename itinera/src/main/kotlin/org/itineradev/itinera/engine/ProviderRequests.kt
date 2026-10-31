package org.itineradev.itinera.engine

import java.lang.reflect.Method
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.FromStep
import org.itineradev.itinera.policy.FromWorkflow
import org.itineradev.itinera.policy.Role
import org.itineradev.itinera.request.AttemptProviderKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Key
import org.itineradev.itinera.workflow.Mode

/**
 * Reads what a service provider's method requests from its parameters, as one of attempt scope may,
 * recording each mistake in [problems]: data marked `@FromWorkflow`, and the rest by its type, the
 * reporter of its mode among them, which [calls] gives.
 */
internal class ProviderRequests<M : Mode>(
    private val calls: ProviderCalls<M>,
    private val problems: Problems,
) {
    private val data = DataRequests(problems)
    private val given: Map<Class<*>, Request<AttemptProviderKind<M>, *>> =
        mapOf(
            JourneyId::class.java to Requests.journeyId(),
            StepName::class.java to Requests.stepName(),
            Attempt::class.java to Requests.attempt(),
        ) + calls.handles

    /** What [method] requests, or `null` when one of its parameters has a mistake. */
    fun of(method: Method, function: KotlinFunction): List<Request<AttemptProviderKind<M>, *>>? =
        function.parameters.map { parameter -> of(method, parameter) }.let(::all)

    private fun of(
        method: Method,
        parameter: DeclaredParameter,
    ): Request<AttemptProviderKind<M>, *>? {
        val fromWorkflow = parameter.parameter.getAnnotation(FromWorkflow::class.java)
        val refused = refusedMarks.find(parameter.parameter::isAnnotationPresent)
        return when {
            fromWorkflow != null ->
                data.data(
                    method,
                    parameter,
                    fromWorkflow.value,
                    fromWorkflow.required,
                    ::FromWorkflowAsked,
                )
            refused != null ->
                data.refused(
                    method,
                    "marks ${nameOf(parameter)} @${refused.simpleName}, which a service provider " +
                        "never requests",
                )
            else -> given[parameter.parameter.type] ?: refused(method, parameter)
        }
    }

    private fun refused(
        method: Method,
        parameter: DeclaredParameter,
    ): Request<AttemptProviderKind<M>, *>? {
        val type = parameter.parameter.type
        return when {
            type.name in calls.otherModes ->
                data.refused(method, calls.otherModes.getValue(type.name))
            type == Contributor::class.java ->
                data.refused(
                    method,
                    "requests a contributor, which a service provider never receives",
                )
            else ->
                data.refused(
                    method,
                    "has a parameter that requests nothing, ${nameOf(parameter)}: mark data " +
                        "@FromWorkflow, and request the journey ID, the step's name, the attempt " +
                        "and the provider's reporter by their types",
                )
        }
    }
}

private val refusedMarks: List<Class<out Annotation>> =
    listOf(
        FromStep::class.java,
        Role::class.java,
        Input::class.java,
        Service::class.java,
        Key::class.java,
    )
