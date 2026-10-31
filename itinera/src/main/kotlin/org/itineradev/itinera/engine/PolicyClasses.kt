package org.itineradev.itinera.engine

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.PolicyName
import org.itineradev.itinera.policy.StepPolicy
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.WorkflowPolicy
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor
import org.itineradev.itinera.workflow.Mode

/**
 * Reads policy classes into policy descriptors of [mode], each class once, recording each mistake
 * in [problems].
 */
internal class PolicyClasses<M : Mode>(
    mode: M,
    calls: HookCalls<M>,
    private val problems: Problems,
) {
    private val hooks = HookMethods(mode, calls, problems)
    private val stepPolicies = HashMap<Class<*>, StepPolicyDescriptor<Any, M>?>()
    private val workflowPolicies = HashMap<Class<*>, WorkflowPolicyDescriptor<Any, M>?>()

    /** The step policy [type] declares, or `null` when it has a mistake. */
    fun stepPolicy(type: Class<*>): StepPolicyDescriptor<Any, M>? =
        if (type in stepPolicies) stepPolicies[type]
        else readStepPolicy(type).also { read -> stepPolicies[type] = read }

    /** The workflow policy [type] declares, or `null` when it has a mistake. */
    fun workflowPolicy(type: Class<*>): WorkflowPolicyDescriptor<Any, M>? =
        if (type in workflowPolicies) workflowPolicies[type]
        else readWorkflowPolicy(type).also { read -> workflowPolicies[type] = read }

    private fun readStepPolicy(type: Class<*>): StepPolicyDescriptor<Any, M>? {
        val named = type.getAnnotation(StepPolicy::class.java)?.name
        val draft = draft(type, named, "@StepPolicy")
        val read = hooks.read(type, labelOf(type, named), ofStep = true)
        return if (draft == null || read == null) null else hooks.define(draft, read).stepPolicy()
    }

    private fun readWorkflowPolicy(type: Class<*>): WorkflowPolicyDescriptor<Any, M>? {
        val named = type.getAnnotation(WorkflowPolicy::class.java)?.name
        val draft = draft(type, named, "@WorkflowPolicy")
        val read = hooks.read(type, labelOf(type, named), ofStep = false)
        return if (draft == null || read == null) null
        else hooks.define(draft, read).workflowPolicy()
    }

    /**
     * A draft of the policy [type] declares, named [named], marked [marker], to define hooks in.
     */
    private fun draft(type: Class<*>, named: String?, marker: String): PolicyDraft<Any, M>? {
        if (!problems.readable(type)) return null
        if (named == null) problems.misdeclared(type.name, "is not marked $marker")
        val name = named?.ifEmpty { type.simpleName }
        val factory = factoryOf(type)
        return if (name == null || factory == null) null
        else PolicyDraft(PolicyName.of(name), factory)
    }

    private fun factoryOf(type: Class<*>): PolicyFactory<Any>? {
        if (Modifier.isAbstract(type.modifiers)) {
            problems.misdeclared(type.name, "cannot be built: it is abstract")
            return null
        }
        val constructor = type.declaredConstructors.find(::takesNothing)
        if (constructor == null)
            problems.misdeclared(
                type.name,
                "has no constructor that takes nothing, which a policy is built with",
            )
        return constructor?.takeIf { taking -> problems.opened(type, taking) }?.let(::factory)
    }
}

/** How the policy [type] marked with the name [named] is named in a violation. */
private fun labelOf(type: Class<*>, named: String?): String =
    named.orEmpty().ifEmpty { type.simpleName }

private fun takesNothing(constructor: Constructor<*>): Boolean = constructor.parameterCount == 0

private fun factory(constructor: Constructor<*>): PolicyFactory<Any> = PolicyFactory {
    constructed(constructor)
}

/** Whether [method] is one a class declares, rather than one the compiler adds. */
internal fun isDeclared(method: Method): Boolean = !method.isBridge && !method.isSynthetic
