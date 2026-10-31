package org.itineradev.itinera.engine

import java.lang.reflect.Method
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.DataBagAccess
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.FromStep
import org.itineradev.itinera.policy.FromWorkflow
import org.itineradev.itinera.policy.Role
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Key

/**
 * Reads what an input adapter's method requests from its parameters, recording each mistake in
 * [problems]: data marked `@FromWorkflow`, the key marked `@Key`, and the rest by its type.
 */
internal class AdapterRequests(private val problems: Problems) {
    private val data = DataRequests(problems)

    /** What [method] requests, or `null` when one of its parameters has a mistake. */
    fun of(method: Method, function: KotlinFunction): List<Request<InputAdapterKind, *>>? =
        function.parameters.map { parameter -> of(method, parameter) }.let(::all)

    private fun of(method: Method, parameter: DeclaredParameter): Request<InputAdapterKind, *>? {
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
            parameter.parameter.isAnnotationPresent(Key::class.java) -> key(method, parameter)
            refused != null ->
                data.refused(
                    method,
                    "marks ${nameOf(parameter)} @${refused.simpleName}, which an input adapter " +
                        "never requests",
                )
            else -> byType(method, parameter)
        }
    }

    private fun key(method: Method, parameter: DeclaredParameter): Request<InputAdapterKind, *>? =
        when {
            parameter.parameter.type != String::class.java ->
                data.refused(
                    method,
                    "marks ${nameOf(parameter)} @Key, though the key is a String",
                )
            isKotlin(method.declaringClass) && admitsNull(parameter.type) ->
                data.refused(
                    method,
                    "requests the key as String?, though the key is always given",
                )
            else -> Requests.key()
        }

    private fun byType(
        method: Method,
        parameter: DeclaredParameter,
    ): Request<InputAdapterKind, *>? {
        val type = parameter.parameter.type
        return givenByType[type]
            ?: if (type == Contributor::class.java)
                data.refused(
                    method,
                    "requests a contributor, which an input adapter never receives",
                )
            else
                data.refused(
                    method,
                    "has a parameter that requests nothing, ${nameOf(parameter)}: mark data " +
                        "@FromWorkflow and the key @Key, and request the step's name, the " +
                        "journey ID and the data bag's access by their types",
                )
    }
}

private val refusedMarks: List<Class<out Annotation>> =
    listOf(FromStep::class.java, Role::class.java, Input::class.java, Service::class.java)

private val givenByType: Map<Class<*>, Request<InputAdapterKind, *>> =
    mapOf(
        StepName::class.java to Requests.stepName(),
        JourneyId::class.java to Requests.journeyId(),
        DataBagAccess::class.java to Requests.dataBagAccess(),
    )
