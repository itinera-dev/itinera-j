package org.itineradev.itinera.workflow.checker

/**
 * One mistake in an annotated workflow, which the run-time reader would refuse as a violation, and
 * where the compiler reports it.
 */
internal sealed interface Finding {
    /** What the compiler reports. */
    val message: String

    /** Where it reports it. */
    val origin: Any
}

/** A declaration itinera cannot use, as the run-time reader's `Misdeclared` says. */
internal data class Misdeclared(
    val subject: String,
    val problem: String,
    override val origin: Any,
) : Finding {
    override val message: String
        get() = "$subject $problem"
}

/** A violation the specification names, such as `duplicate step name`. */
internal data class Named(
    val named: String,
    val detail: String,
    override val origin: Any,
) : Finding {
    override val message: String
        get() = "$named: $detail"
}

/** How [class] is named in a mistake. */
internal fun subjectOf(`class`: ClassSymbol): String = `class`.name

/** How [function] is named in a mistake. */
internal fun subjectOf(function: FunctionSymbol): String =
    if (function.isConstructor) "the constructor of ${function.owner.name}"
    else "${function.owner.name}.${function.jvmName}"

/** The mistakes found while a workflow class is read, in the order found. */
internal class Findings {
    private val recorded = mutableListOf<Finding>()

    val all: List<Finding>
        get() = recorded.toList()

    val found: Boolean
        get() = recorded.isNotEmpty()

    /** How many mistakes are recorded so far, to tell whether a later read finds any. */
    val count: Int
        get() = recorded.size

    operator fun plusAssign(finding: Finding) {
        recorded += finding
    }

    operator fun plusAssign(findings: Iterable<Finding>) {
        recorded += findings
    }

    fun misdeclared(`class`: ClassSymbol, problem: String) {
        recorded += Misdeclared(subjectOf(`class`), problem, `class`.origin)
    }

    fun misdeclared(function: FunctionSymbol, problem: String) {
        recorded += Misdeclared(subjectOf(function), problem, function.origin)
    }

    fun modeNotAccepted(part: String, mode: Mode, origin: Any) {
        recorded +=
            Named("mode not accepted", "$part is not ${mode.written}, as the workflow is", origin)
    }
}
