package org.itineradev.itinera.workflow.checker

/** Reads the ID generator of a workflow class, recording each mistake in [findings]. */
internal class IdGenerators(private val findings: Findings) {
    /** Reads the ID generator [type] declares, if any. */
    fun read(type: ClassSymbol) {
        val generators =
            generateSequence(type, ClassSymbol::superclass)
                .flatMap(ClassSymbol::methods)
                .filter(::isIdGenerator)
                .toList()
        if (generators.size > 1) findings.misdeclared(type, "has several ID generators")
        generators.singleOrNull()?.let(::checked)
    }

    private fun checked(method: FunctionSymbol) {
        if (method.isSuspend)
            findings.misdeclared(method, "is suspend, which an ID generator never is")
        val parameters = method.parameters.map(ParameterSymbol::jvmClass)
        val takes = parameters.isEmpty() || parameters == listOf(Names.DATA_BAG)
        if (!takes)
            findings.misdeclared(
                method,
                "takes what an ID generator cannot: it takes nothing or a DataBag",
            )
        if (!method.isSuspend && takes) returns(findings, method, Names.STRING)
    }
}

private fun isIdGenerator(method: FunctionSymbol): Boolean = method.isMarked(Names.ID_GENERATOR)
