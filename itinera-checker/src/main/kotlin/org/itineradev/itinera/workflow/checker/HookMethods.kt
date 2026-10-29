package org.itineradev.itinera.workflow.checker

/** Reads the hook methods of policy classes of [mode], recording each mistake in [findings]. */
internal class HookMethods(private val mode: Mode, private val findings: Findings) {
    private val requests = HookRequests(mode, findings)

    /**
     * The hooks the policy class [type] defines with its methods, a step policy's if [ofStep], or
     * `null` when they have a mistake. Mistakes name the policy [policy].
     */
    fun read(type: ClassSymbol, policy: String, ofStep: Boolean): List<ReadHook>? {
        val before = findings.count
        generateSequence(type, ClassSymbol::superclass)
            .flatMap(ClassSymbol::methods)
            .filter(::isHiddenHook)
            .forEach(::hidden)
        val marked = type.publicMethods.mapNotNull(::marked)
        if (marked.isEmpty())
            findings.misdeclared(type, "defines no hook, though a policy defines at least one")
        definedTwice(type, marked)
        val read =
            marked
                .filter { mark -> ofKind(mark, ofStep) }
                .mapNotNull { mark -> hookMethod(policy, mark) }
        return read.takeIf { findings.count == before }
    }

    private fun hidden(method: FunctionSymbol) {
        findings.misdeclared(
            method,
            "is marked ${written(hooksOf(method))}, though a hook is a public method",
        )
    }

    private fun marked(method: FunctionSymbol): Marked? {
        val hooks = hooksOf(method)
        if (hooks.size > 1)
            findings.misdeclared(method, "is marked ${written(hooks)}, though a method is one hook")
        val hook = hooks.singleOrNull() ?: return null
        return Marked(hook, method)
    }

    private fun definedTwice(type: ClassSymbol, marked: List<Marked>) {
        for (hook in marked.groupBy(Marked::hook).filterValues(::isShared).keys) findings
            .misdeclared(
                type,
                "defines ${hook.hook} in several methods, though a policy defines each hook once",
            )
    }

    private fun ofKind(mark: Marked, ofStep: Boolean): Boolean {
        val ofKind = mark.hook.ofStep == ofStep
        if (!ofKind)
            findings.misdeclared(
                mark.method,
                "is marked ${mark.hook.written}, which only a " +
                    (if (mark.hook.ofStep) "step" else "workflow") +
                    " policy defines",
            )
        return ofKind
    }

    private fun hookMethod(policy: String, mark: Marked): ReadHook? {
        val method = mark.method
        val inMode = method.isSuspend == mode.suspends
        if (!inMode)
            findings.modeNotAccepted(
                "hook ${mark.hook.hook} of policy $policy",
                mode,
                method.origin,
            )
        val returns = returns(mark.hook, method)
        val asks = requests.of(mark.hook, method)
        return if (inMode && returns && asks != null) ReadHook(mark.hook, asks) else null
    }

    private fun returns(hook: Hook, method: FunctionSymbol): Boolean {
        val returns = hook.returns ?: return returnsNothing(method)
        return returns(findings, method, returns)
    }

    private fun returnsNothing(method: FunctionSymbol): Boolean {
        val nothing = method.returns == VoidUse || method.returnClass?.name == Names.UNIT
        if (!nothing)
            findings.misdeclared(
                method,
                "returns ${method.returnName}, though a workflow hook returns nothing",
            )
        return nothing
    }
}

/** A method of a policy class and the [hook] it is marked as. */
private class Marked(val hook: Hook, val method: FunctionSymbol)

private fun hooksOf(method: FunctionSymbol): List<Hook> =
    Hook.entries.filter { hook -> method.isMarked(hook.annotation) }

private fun written(hooks: List<Hook>): String =
    hooks.joinToString(" and ", transform = Hook::written)

private fun isHiddenHook(method: FunctionSymbol): Boolean =
    !method.isPublic && hooksOf(method).isNotEmpty()

private fun isShared(marks: List<Marked>): Boolean = marks.size > 1
