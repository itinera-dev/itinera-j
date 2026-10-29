package org.itineradev.itinera.workflow.checker

/**
 * Reads the requests that parameters of steps and hooks make, recording each mistake in [findings]:
 * the checks the readers of both share.
 */
internal class DataRequests(private val findings: Findings) {
    /**
     * The request of data that [parameter] of [declaration] makes under the key [written], or its
     * name when that is empty, required or not as [required] and its type say; or `null` when it
     * has a mistake.
     */
    fun data(
        declaration: FunctionSymbol,
        parameter: ParameterSymbol,
        written: String,
        required: Boolean,
    ): Asked? {
        val form = WrittenRequest(parameter.type, required, declaration.owner.isKotlin)
        val key = written.ifEmpty { parameter.name }
        val refusal = whyNotRequestable(form.type())
        if (refusal != null) findings.misdeclared(declaration, "requests $key: $refusal")
        return Other.takeIf { refusal == null }
    }

    /** Records [problem] in [declaration], which therefore requests nothing. */
    fun refused(declaration: FunctionSymbol, problem: String): Asked? {
        findings.misdeclared(declaration, problem)
        return null
    }
}

/**
 * How a request's parameter is written: its declared [type], whether its annotation says it is
 * [required][requiredWritten], and whether it is in a Kotlin class, whose nullable types make a
 * request optional, where a Java class writes `Optional`.
 */
internal class WrittenRequest(
    private val type: TypeUse,
    requiredWritten: Boolean,
    private val kotlin: Boolean,
) {
    private val optional = !requiredWritten && if (kotlin) admitsNull(type) else isOptional(type)

    /**
     * The type the value is read as: what an `Optional` holds, which is never `null`, or the type
     * itself.
     */
    fun type(): TypeUse =
        if (optional && !kotlin)
            excludingNull((type as ClassUse).arguments.singleOrNull() ?: AnyUse)
        else type
}

private fun isOptional(type: TypeUse): Boolean = classOf(type)?.name == Names.OPTIONAL

/** [type], though `null` is not of it at the top level. */
private fun excludingNull(type: TypeUse): TypeUse =
    when (type) {
        is ClassUse -> type.copy(nullable = false)
        is ArrayUse -> type.copy(nullable = false)
        is VariableUse -> type.copy(nullness = Nullness.NON_NULL)
        AnyUse,
        VoidUse -> type
    }

/**
 * What a failing attempt left that a parameter of [type] requests by its type, in a Kotlin class
 * when [kotlin]: `the failure's reason` or `the error`, or `null` for neither.
 */
internal fun failedBy(type: TypeUse, kotlin: Boolean): String? =
    when (classOf(WrittenRequest(type, false, kotlin).type())?.name) {
        Names.REASON -> "the failure's reason"
        Names.THROWABLE -> "the error"
        else -> null
    }
