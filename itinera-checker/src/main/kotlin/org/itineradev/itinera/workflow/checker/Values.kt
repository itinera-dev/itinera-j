package org.itineradev.itinera.workflow.checker

/** The classes of the copies a receiver gets, which a request may name though they are concrete. */
internal val copyClasses =
    setOf("java.util.ArrayList", "java.util.LinkedHashSet", "java.util.LinkedHashMap")

/** The classes whose instances are immutable values, matched exactly. */
internal val sharedClasses =
    setOf(
        "java.lang.Boolean",
        "java.lang.Character",
        Names.STRING,
        "java.lang.Byte",
        "java.lang.Short",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Float",
        "java.lang.Double",
        "java.math.BigInteger",
        "java.math.BigDecimal",
        "java.util.UUID",
        "java.net.URI",
        "java.time.Instant",
        "java.time.LocalDate",
        "java.time.LocalTime",
        "java.time.LocalDateTime",
        "java.time.OffsetDateTime",
        "java.time.OffsetTime",
        "java.time.ZonedDateTime",
        "java.time.Duration",
        "java.time.Period",
        "java.time.Year",
        "java.time.YearMonth",
        "java.time.MonthDay",
    )

/**
 * Why no value can be of [type] at some level, as the run-time reader says it, or `null` when some
 * value can.
 */
internal fun whyNotRequestable(type: TypeUse): String? =
    when (type) {
        AnyUse,
        VoidUse -> null
        is VariableUse ->
            "A type token needs a type with no type variable, but ${written(type)} is one."
        is ArrayUse ->
            "No value is an array, such as ${type.simpleName}: a value holds a list instead."
        is ClassUse -> whyNotValue(type.raw) ?: whyNotKeyed(type) ?: argumentsRefused(type)
    }

private fun argumentsRefused(type: ClassUse): String? =
    type.arguments.firstNotNullOfOrNull(::whyNotRequestable)

private fun whyNotValue(type: ClassSymbol): String? = if (canBeValue(type)) null else whyNot(type)

private fun canBeValue(type: ClassSymbol): Boolean =
    when {
        type.isInterface || type.name in copyClasses || type.name == Names.OBJECT -> true
        isCollection(type) -> false
        isValueShaped(type) -> true
        type.isAbstract -> holdsNoFields(type)
        else -> false
    }

private fun isCollection(type: ClassSymbol): Boolean =
    type.isSubtypeOf(Names.COLLECTION) || type.isSubtypeOf(Names.MAP)

private fun isValueShaped(type: ClassSymbol): Boolean =
    when {
        type.name in sharedClasses || type.isSubtypeOf(Names.ZONE_ID) -> true
        type.isEnum -> isPlatform(type) || type.instanceFields.all(::isTrue)
        type.isKotlin -> type.kotlinData ?: true
        else -> type.isRecord
    }

private fun isTrue(final: Boolean): Boolean = final

private fun holdsNoFields(type: ClassSymbol): Boolean =
    generateSequence(type, ClassSymbol::superclass).all(::declaresNoFields)

private fun declaresNoFields(type: ClassSymbol): Boolean = type.instanceFields.isEmpty()

private fun whyNot(type: ClassSymbol): String =
    when {
        type.isRecord || type.isEnum ->
            "No value is a ${type.name}: a record or an enum constant is a value only when its " +
                "module opens its package to org.itineradev.itinera, and an enum constant only " +
                "when its fields are final."
        isCollection(type) ->
            "No value is a ${type.name}: a value's lists, sets and maps are read as List, Set and " +
                "Map, or as ArrayList, LinkedHashSet and LinkedHashMap."
        else ->
            "No value is a ${type.name}: values are records, data classes, enum constants, " +
                "lists, sets, maps with string keys, and the JDK's immutable values."
    }
