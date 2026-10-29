package org.itineradev.itinera.engine

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.itineradev.itinera.workflow.Async
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Violation

/** Reads the hook methods of policy classes of [mode], recording each mistake in [problems]. */
internal class HookMethods<M : Mode>(
    private val mode: M,
    private val calls: HookCalls<M>,
    private val problems: Problems,
) {
    private val requests = HookRequests(calls, problems)

    /**
     * The hooks the policy class [type] defines with its methods, a step policy's if [ofStep], or
     * `null` when they have a mistake. Violations name the policy [policy].
     */
    fun read(type: Class<*>, policy: String, ofStep: Boolean): List<Marked<HookMethod>>? {
        val before = problems.count
        generateSequence(type, Class<*>::getSuperclass)
            .flatMap(::declaredMethods)
            .filter(::isHiddenHook)
            .forEach(::hidden)
        val marked = type.methods.map(::bridged).filter(::isDeclared).mapNotNull(::marked)
        if (marked.isEmpty())
            problems.misdeclared(type.name, "defines no hook, though a policy defines at least one")
        definedTwice(type, marked)
        val read =
            marked
                .filter { mark -> ofKind(mark, ofStep) }
                .mapNotNull { mark -> hookMethod(policy, mark) }
        return if (problems.count == before) read else null
    }

    /** [draft], with the hooks [read] defined. */
    fun define(draft: PolicyDraft<Any, M>, read: List<Marked<HookMethod>>): PolicyDraft<Any, M> {
        read.forEach { mark -> define(draft, mark) }
        return draft
    }

    private fun define(draft: PolicyDraft<Any, M>, mark: Marked<HookMethod>) {
        val hook = mark.method
        when (mark.hook) {
            HookAnnotation.ON_STEP_SUCCESS ->
                draft.onStepSuccess(hook.requests(), calls.stepHook(hook))
            HookAnnotation.ON_STEP_FAILURE ->
                draft.onStepFailure(hook.requests(), calls.stepHook(hook))
            HookAnnotation.ON_STEP_RETRY -> draft.onStepRetry(hook.requests(), calls.stepHook(hook))
            HookAnnotation.ON_STEP_ABNORMAL_TERMINATION ->
                draft.onStepAbnormalTermination(hook.requests(), calls.stepHook(hook))
            HookAnnotation.ON_WORKFLOW_SUCCESS ->
                draft.onWorkflowSuccess(hook.requests(), calls.workflowHook(hook))
            HookAnnotation.ON_WORKFLOW_FAILURE ->
                draft.onWorkflowFailure(hook.requests(), calls.workflowHook(hook))
        }
    }

    private fun hidden(method: Method) {
        problems.misdeclared(
            method.toString(),
            "is marked ${written(hooksOf(method))}, though a hook is a public method",
        )
    }

    private fun marked(method: Method): Marked<Method>? {
        val hooks = hooksOf(method)
        if (hooks.size > 1)
            problems.misdeclared(
                method.toString(),
                "is marked ${written(hooks)}, though a method is one hook",
            )
        return hooks.singleOrNull()?.let { hook -> Marked(hook, method) }
    }

    private fun definedTwice(type: Class<*>, marked: List<Marked<Method>>) {
        marked.groupBy(Marked<Method>::hook).filterValues(::isSharedMarks).keys.forEach { hook ->
            definedTwice(type, hook)
        }
    }

    private fun definedTwice(type: Class<*>, hook: HookAnnotation) {
        problems.misdeclared(
            type.name,
            "defines ${hook.hook} in several methods, though a policy defines each hook once",
        )
    }

    private fun ofKind(mark: Marked<Method>, ofStep: Boolean): Boolean {
        val ofKind = mark.hook.ofStep == ofStep
        if (!ofKind)
            problems.misdeclared(
                mark.method.toString(),
                "is marked ${mark.hook.written}, which only a " +
                    (if (mark.hook.ofStep) "step" else "workflow") +
                    " policy defines",
            )
        return ofKind
    }

    private fun hookMethod(policy: String, mark: Marked<Method>): Marked<HookMethod>? {
        val method = mark.method
        val function = functionOf(method)
        val inMode = function.isSuspend == (mode == Async)
        if (!inMode)
            problems += Violation.ModeNotAccepted("hook ${mark.hook.hook} of policy $policy", mode)
        val returns = returns(mark.hook, method, function)
        val asks = requests.of(mark.hook, method, function)
        val opened = problems.opened(method.declaringClass, method)
        val valid = inMode && returns && opened
        return if (valid && asks != null) Marked(mark.hook, HookMethod(method, asks)) else null
    }

    private fun returns(hook: HookAnnotation, method: Method, function: KotlinFunction): Boolean {
        val returns = hook.returns ?: return returnsNothing(method, function)
        return problems.returns(method, function, returns)
    }

    private fun returnsNothing(method: Method, function: KotlinFunction): Boolean {
        val type = function.returnType
        val nothing = type == Void.TYPE || type == Unit::class.java
        if (!nothing)
            problems.misdeclared(
                method.toString(),
                "returns ${type.simpleName}, though a workflow hook returns nothing",
            )
        return nothing
    }
}

/** A method of a policy class, or what was read from it, and the [hook] it is marked as. */
internal class Marked<T>(val hook: HookAnnotation, val method: T)

private fun hooksOf(method: Method): List<HookAnnotation> =
    HookAnnotation.entries.filter { hook -> method.isAnnotationPresent(hook.annotation) }

private fun written(hooks: List<HookAnnotation>): String =
    hooks.joinToString(" and ", transform = HookAnnotation::written)

private fun isHiddenHook(method: Method): Boolean =
    !Modifier.isPublic(method.modifiers) && isDeclared(method) && hooksOf(method).isNotEmpty()

private fun isSharedMarks(marks: List<Marked<Method>>): Boolean = isShared(marks.size)
