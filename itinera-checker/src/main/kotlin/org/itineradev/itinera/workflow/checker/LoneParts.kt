package org.itineradev.itinera.workflow.checker

/**
 * Every mistake of [type] as a part read alone, as `StepDescriptor.builder(name, type)` and the
 * other factories of one annotated part read it: once for each of `@Step`, `@RescueStep`,
 * `@StepPolicy` and `@WorkflowPolicy` it is marked with, in the mode its own methods say, since the
 * factory that reads it decides its mode.
 */
internal fun loneMistakesOf(type: ClassSymbol): List<Finding> =
    LonePart.entries
        .filter { part -> type.isMarked(part.mark) }
        .flatMap { part -> part.mistakes(type) }

/** What a class may be marked as, with how it is read alone. */
private enum class LonePart(val mark: String) {
    STEP(Names.STEP) {
        override fun read(type: ClassSymbol, mode: Mode, findings: Findings) {
            StepClasses(mode, findings).step(type.simpleName, type)
        }
    },
    RESCUE_STEP(Names.RESCUE_STEP) {
        override fun read(type: ClassSymbol, mode: Mode, findings: Findings) {
            StepClasses(mode, findings).rescue("rescue step ${type.simpleName}", type)
        }
    },
    STEP_POLICY(Names.STEP_POLICY) {
        override fun read(type: ClassSymbol, mode: Mode, findings: Findings) {
            PolicyClasses(mode, findings).stepPolicy(type)
        }
    },
    WORKFLOW_POLICY(Names.WORKFLOW_POLICY) {
        override fun read(type: ClassSymbol, mode: Mode, findings: Findings) {
            PolicyClasses(mode, findings).workflowPolicy(type)
        }
    };

    /** Reads [type] in [mode], recording each mistake in [findings]. */
    abstract fun read(type: ClassSymbol, mode: Mode, findings: Findings)

    /** Every mistake of [type] read alone. */
    fun mistakes(type: ClassSymbol): List<Finding> {
        val findings = Findings()
        read(type, modeOf(type), findings)
        return findings.all
    }
}

/**
 * The mode [type]'s own methods say: asynchronous when its methods named `run`, or its hooks, are
 * all `suspend`, and there is one at least, and synchronous otherwise.
 */
private fun modeOf(type: ClassSymbol): Mode {
    val methods = type.publicMethods.filter(::isPartMethod)
    return if (methods.isNotEmpty() && methods.all(FunctionSymbol::isSuspend)) Mode.ASYNC
    else Mode.SYNC
}

private fun isPartMethod(method: FunctionSymbol): Boolean =
    isRun(method) || hooksOf(method).isNotEmpty()
