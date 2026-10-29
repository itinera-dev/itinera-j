package org.itineradev.itinera.workflow.checker

import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violation

/** [violation] as the checker reports the same mistake. */
@Suppress("CyclomaticComplexMethod")
internal fun rendered(violation: Violation): String =
    when (violation) {
        is Violation.Misdeclared -> "${subject(violation.declaration)} ${violation.problem}"
        is Violation.ModeNotAccepted ->
            "mode not accepted: ${violation.part} is not ${written(violation)}, as the workflow is"
        is Violation.DuplicateStepName ->
            "duplicate step name: two steps are named ${violation.step}"
        is Violation.RescuedStepRetried ->
            "rescued step retried: step ${violation.step} has a rescue step and a retry budget " +
                "above 0"
        is Violation.StepHookDefinedTwice ->
            "hook defined twice: step policies ${violation.policies.joinToString(", ")} of step " +
                "${violation.step} all define ${violation.hook}"
        is Violation.WorkflowHookDefinedTwice ->
            "hook defined twice: workflow policies ${violation.policies.joinToString(", ")} all " +
                "define ${violation.hook}"
        is Violation.RoleNotProvided ->
            "role not provided: policy ${violation.policy} requests role ${violation.role.name}, " +
                "which the workflow does not provide"
        is Violation.DuplicateAdapterName ->
            "duplicate adapter name: two input adapters are named ${violation.adapter}"
        is Violation.StepAdaptedTwice ->
            "step adapted twice: step ${violation.step} has several input adapters, " +
                violation.adapters.joinToString(", ")
        is Violation.InputAdapterForUnknownStep ->
            "input adapter for unknown step: input adapter ${violation.adapter} names step " +
                "${violation.step}, which the workflow does not have"
        is Violation.ServiceRequestedTwice ->
            "service requested twice: step ${violation.step} requests service ${violation.key} twice"
        is Violation.ServiceNotProvided ->
            "service not provided: no service provider serves service ${violation.key} to step " +
                "${violation.step}"
        is Violation.ServiceProvidedTwice ->
            "service provided twice: service providers ${violation.providers.joinToString(", ")} " +
                "all serve service ${violation.key} to " +
                (violation.step?.let(::stepNamed) ?: "every step")
        is Violation.DuplicateProviderName ->
            "duplicate provider name: two service providers are named ${violation.provider}"
        is Violation.ServiceProviderForUnknownStep ->
            "service provider for unknown step: service provider ${violation.provider} names " +
                "step ${violation.step}, which the workflow does not have"
        is Violation.WrongServiceType ->
            "wrong service type: service provider ${violation.provider} serves step " +
                "${violation.step} another type than it requests as ${violation.key}"
        is Violation.AttemptRequestInJourneyScope ->
            "attempt request in journey scope: service provider ${violation.provider} requests " +
                "the step's name or the attempt, which only a provider of attempt scope receives"
        is Violation.NotAValue,
        Violation.ReservedMetadataKey -> violation.toString()
    }

private fun written(violation: Violation.ModeNotAccepted): String =
    if (violation.accepted == Sync) "synchronous" else "asynchronous"

/**
 * How the checker names a declaration that Java reflection writes, such as `public final
 * com.shop.Charge.run(long)`: a method by its class and name, a constructor as the constructor of
 * its class, and anything else as it is written.
 */
private fun subject(declaration: String): String {
    if ('(' !in declaration) return declaration
    val named = declaration.substringBefore('(').substringAfterLast(' ')
    return if (isClass(named)) "the constructor of $named" else named
}

private fun isClass(name: String): Boolean = runCatching {
    Class.forName(name, false, Violation::class.java.classLoader)
}
    .isSuccess

private fun stepNamed(step: StepName): String = "step $step"
