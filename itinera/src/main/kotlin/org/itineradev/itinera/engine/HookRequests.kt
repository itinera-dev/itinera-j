package org.itineradev.itinera.engine

import java.lang.reflect.Method
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.FromStep
import org.itineradev.itinera.policy.FromWorkflow
import org.itineradev.itinera.policy.RetryCause
import org.itineradev.itinera.policy.Role
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Mode

/**
 * Reads what a hook method of a policy class requests from its parameters, recording each mistake
 * in [problems]: data marked `@FromStep` or `@FromWorkflow`, roles marked `@Role`, and the rest by
 * its type, each only where its hook's kind may request it.
 */
internal class HookRequests<M : Mode>(
    private val calls: HookCalls<M>,
    private val problems: Problems,
) {
    private val data = DataRequests(problems)

    /**
     * What [method], marked [hook], requests, or `null` when one of its parameters has a mistake.
     */
    fun of(hook: HookAnnotation, method: Method, function: KotlinFunction): List<Request<*, *>>? =
        function.parameters.map { parameter -> of(hook, method, parameter) }.let(::all)

    private fun of(
        hook: HookAnnotation,
        method: Method,
        parameter: DeclaredParameter,
    ): Request<*, *>? {
        val fromStep = parameter.parameter.getAnnotation(FromStep::class.java)
        val fromWorkflow = parameter.parameter.getAnnotation(FromWorkflow::class.java)
        return when {
            fromStep != null -> fromStep(hook, method, parameter, fromStep)
            fromWorkflow != null ->
                data.data<Any?>(
                    method,
                    parameter,
                    fromWorkflow.value,
                    fromWorkflow.required,
                    ::FromWorkflowAsked,
                )
            parameter.parameter.isAnnotationPresent(Role::class.java) -> role(method, parameter)
            parameter.parameter.isAnnotationPresent(Input::class.java) ->
                data.refused<Any?>(
                    method,
                    "marks ${nameOf(parameter)} @Input, though a hook requests data with " +
                        "@FromStep or @FromWorkflow",
                )
            parameter.parameter.isAnnotationPresent(Service::class.java) ->
                data.refused<Any?>(
                    method,
                    "marks ${nameOf(parameter)} @Service, though a hook never requests a service",
                )
            else -> byType(hook, method, parameter)
        }
    }

    private fun fromStep(
        hook: HookAnnotation,
        method: Method,
        parameter: DeclaredParameter,
        fromStep: FromStep,
    ): Request<*, *>? =
        if (hook.ofStep)
            data.data<Any?>(method, parameter, fromStep.value, fromStep.required, ::FromStepAsked)
        else data.refused<Any?>(method, "requests data from the step, which only a step hook can")

    private fun role(method: Method, parameter: DeclaredParameter): Request<*, *>? {
        val type = parameter.parameter.type
        return when {
            !isRole(type) ->
                data.refused<Any?>(
                    method,
                    "requests a role of ${type.name}, which is not an interface marked @Role",
                )
            isKotlin(method.declaringClass) && admitsNull(parameter.type) ->
                data.refused<Any?>(
                    method,
                    "requests role ${type.simpleName} as a type that admits null, though a role " +
                        "is always given",
                )
            else -> requestOf<Any?, Any?>(RoleAsked(type), Answer.ITSELF)
        }
    }

    private fun byType(
        hook: HookAnnotation,
        method: Method,
        parameter: DeclaredParameter,
    ): Request<*, *>? {
        val type = parameter.parameter.type
        val form = WrittenRequest(parameter.type, false, isKotlin(method.declaringClass))
        val given = givenByType[type]
        val failed = failedBy(form)
        return when {
            given != null -> given(hook, method, given)
            failed != null -> failed(hook, method, form, failed)
            type in calls.handles -> calls.handles[type]
            type.name in calls.otherModes ->
                data.refused<Any?>(method, calls.otherModes.getValue(type.name))
            else ->
                data.refused<Any?>(
                    method,
                    "has a parameter that requests nothing, ${nameOf(parameter)}: mark data " +
                        "@FromStep or @FromWorkflow and a role @Role, and request the rest by " +
                        "its type",
                )
        }
    }

    private fun given(hook: HookAnnotation, method: Method, given: Given): Request<*, *>? =
        if (hook in given.hooks) given.request
        else data.refused<Any?>(method, "requests ${given.what}, which only ${given.who} can")

    private fun failed(
        hook: HookAnnotation,
        method: Method,
        form: WrittenRequest,
        failed: Failed,
    ): Request<*, *>? =
        if (hook.ofStep) data.failed<Any?>(method, form, failed)
        else data.refused<Any?>(method, "requests ${failed.what}, which only a step hook can")
}

/** A request a hook makes by its parameter's type, [what] it is, and the [hooks] that may. */
private class Given(val request: Request<*, *>, val what: String, val hooks: Set<HookAnnotation>) {
    /** Which hooks may, when not every hook may: one hook, or every step hook. */
    val who: String
        get() = hooks.singleOrNull()?.hook ?: "a step hook"
}

private val everyHook = HookAnnotation.entries.toSet()

private val stepHooks = HookAnnotation.entries.filter(HookAnnotation::ofStep).toSet()

private val givenByType: Map<Class<*>, Given> =
    mapOf(
        Contributor::class.java to Given(Requests.contributor(), "a contributor", everyHook),
        JourneyId::class.java to Given(Requests.journeyId(), "the journey ID", everyHook),
        StepName::class.java to Given(Requests.stepName(), "the step's name", stepHooks),
        Attempt::class.java to Given(Requests.attempt(), "the attempt", stepHooks),
        FailureCause::class.java to
            Given(
                Requests.failureCause(),
                "why the step failed",
                setOf(HookAnnotation.ON_STEP_FAILURE),
            ),
        RetryCause::class.java to
            Given(
                Requests.retryCause(),
                "why the step is retried",
                setOf(HookAnnotation.ON_STEP_RETRY),
            ),
    )
