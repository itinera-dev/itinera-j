package org.itineradev.itinera.engine

import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.report.ReporterOf
import org.itineradev.itinera.service.ProviderDescriptor
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** Finds every part of an instance in a mode the executor of [parts] does not accept. */
internal class Modes<M : Mode>(private val parts: Parts<M>) {
    fun of(instance: WorkflowInstance<M>): List<Violation> =
        workflow(instance.descriptor) +
            instance.descriptor.steps.filterNot(::accepts).map { step -> step(step, parts.mode) } +
            instance.descriptor.steps.filter(::isRescuedInAnotherMode).map { step ->
                rescue(step, parts.mode)
            } +
            hooksOf(instance.descriptor).filterNot(::accepts).map { hook ->
                hook(hook, parts.mode)
            } +
            instance.descriptor.inputAdapters.filterNot(::accepts).map { adapter ->
                adapter(adapter, parts.mode)
            } +
            instance.descriptor.providers.filterNot(::accepts).map { provider ->
                provider(provider, parts.mode)
            } +
            instance.reporters.withIndex().filterNot(::accepts).map { reporter ->
                reporter(reporter, parts.mode)
            }

    private fun workflow(descriptor: WorkflowDescriptor<*, *>): List<Violation> =
        if (descriptor.mode == parts.mode) emptyList()
        else listOf(Violation.ModeNotAccepted("workflow ${descriptor.name}", parts.mode))

    private fun accepts(step: StepDescriptor<*>): Boolean = parts.accepts(step.factory)

    private fun isRescuedInAnotherMode(step: StepDescriptor<*>): Boolean {
        val rescue = step.rescue
        return rescue != null && !parts.accepts(rescue.factory)
    }

    private fun accepts(hook: DefinedHook<*>): Boolean = parts.accepts(hook.definition.call)

    private fun accepts(adapter: InputAdapterDescriptor<*, *>): Boolean =
        parts.accepts(adapter.call)

    private fun accepts(provider: ProviderDescriptor<*, *>): Boolean =
        parts.accepts(callOf(provider))

    private fun accepts(reporter: IndexedValue<ReporterOf<*>?>): Boolean =
        parts.accepts(reporter.value)
}

private fun step(step: StepDescriptor<*>, mode: Mode): Violation =
    Violation.ModeNotAccepted("step ${step.name}", mode)

private fun rescue(step: StepDescriptor<*>, mode: Mode): Violation =
    Violation.ModeNotAccepted("rescue step ${rescueOf(step.name)}", mode)

private fun hook(hook: DefinedHook<*>, mode: Mode): Violation =
    Violation.ModeNotAccepted(hook.toString(), mode)

private fun adapter(adapter: InputAdapterDescriptor<*, *>, mode: Mode): Violation =
    Violation.ModeNotAccepted("input adapter ${adapter.name}", mode)

private fun provider(provider: ProviderDescriptor<*, *>, mode: Mode): Violation =
    Violation.ModeNotAccepted("service provider ${provider.name}", mode)

private fun reporter(reporter: IndexedValue<ReporterOf<*>?>, mode: Mode): Violation =
    Violation.ModeNotAccepted("reporter ${reporter.index + 1}", mode)
