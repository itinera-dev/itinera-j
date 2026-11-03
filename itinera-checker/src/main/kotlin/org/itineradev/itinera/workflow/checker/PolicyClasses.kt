package org.itineradev.itinera.workflow.checker

/** What a policy class declares, once read without a mistake: its name and the hooks it defines. */
internal class ReadPolicy(val name: String, val hooks: List<ReadHook>)

/** A hook a policy defines, and what it requests. */
internal class ReadHook(val hook: Hook, val asks: List<Asked>)

/**
 * Reads the policy classes of a workflow class in [mode], each class once, recording each mistake
 * in [findings].
 */
internal class PolicyClasses(mode: Mode, private val findings: Findings) {
    private val hooks = HookMethods(mode, findings)
    private val stepPolicies = HashMap<String, ReadPolicy?>()
    private val workflowPolicies = HashMap<String, ReadPolicy?>()

    /** The step policy [type] declares, or `null` when it has a mistake. */
    fun stepPolicy(type: ClassSymbol): ReadPolicy? =
        once(stepPolicies, type) { read(type, Names.STEP_POLICY, ofStep = true) }

    /** The workflow policy [type] declares, or `null` when it has a mistake. */
    fun workflowPolicy(type: ClassSymbol): ReadPolicy? =
        once(workflowPolicies, type) { read(type, Names.WORKFLOW_POLICY, ofStep = false) }

    private fun once(
        read: MutableMap<String, ReadPolicy?>,
        type: ClassSymbol,
        reading: () -> ReadPolicy?,
    ): ReadPolicy? {
        if (type.name !in read) read[type.name] = reading()
        return read[type.name]
    }

    private fun read(type: ClassSymbol, mark: String, ofStep: Boolean): ReadPolicy? {
        val named = type.annotation(mark)?.string("name")
        if (named == null) findings.misdeclared(type, "is not marked @${simpleNameOf(mark)}")
        val built = builtWithNothing(findings, type, "policy")
        val read = hooks.read(type, named.orEmpty().ifEmpty { type.simpleName }, ofStep)
        val name = named?.ifEmpty { type.simpleName }
        return if (name == null || !built || read == null) null else ReadPolicy(name, read)
    }
}

/**
 * Whether the [part] [type] can be built with a constructor that takes nothing, recording in
 * [findings] why not, when it is abstract or has none.
 */
internal fun builtWithNothing(findings: Findings, type: ClassSymbol, part: String): Boolean {
    if (type.isAbstract) {
        findings.misdeclared(type, "cannot be built: it is abstract")
        return false
    }
    val built = type.constructors.any(::takesNothing)
    if (!built)
        findings.misdeclared(
            type,
            "has no constructor that takes nothing, which a $part is built with",
        )
    return built
}

private fun takesNothing(constructor: FunctionSymbol): Boolean =
    constructor.parameters.isEmpty() && !constructor.capturesEnclosing
