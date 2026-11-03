package org.itineradev.itinera.workflow.checker

/**
 * Whether the workflow class [type], or a part it names, was compiled from Kotlin or extends a
 * class that was: javac reads such a class only as Java reflection does, not as itinera-kotlin
 * does, so its mistakes are left to the run-time checks.
 */
internal fun namesKotlinPart(type: ClassSymbol): Boolean =
    (listOf(type) + partsOf(type)).any(::isFromKotlin)

private fun isFromKotlin(type: ClassSymbol): Boolean =
    generateSequence(type, ClassSymbol::superclass).any(ClassSymbol::isKotlin)

private fun partsOf(type: ClassSymbol): List<ClassSymbol> =
    type.annotations(Names.WITH_STEP).flatMap(::stepParts) +
        type.annotation(Names.WORKFLOW)?.let(::workflowParts).orEmpty()

private fun stepParts(step: AnnotationSymbol): List<ClassSymbol> =
    listOf(step.type("step")) + rescueOf(step) + step.types("policies")

/** The rescue step [step] names, if any: `ItineraRescueStep` itself names none. */
private fun rescueOf(step: AnnotationSymbol): List<ClassSymbol> =
    listOf(step.type("rescue")).filterNot(::isNoRescue)

private fun isNoRescue(rescue: ClassSymbol): Boolean = rescue.name == Names.ITINERA_RESCUE_STEP

private fun workflowParts(workflow: AnnotationSymbol): List<ClassSymbol> =
    workflow.types("policies") + workflow.types("reporters")
