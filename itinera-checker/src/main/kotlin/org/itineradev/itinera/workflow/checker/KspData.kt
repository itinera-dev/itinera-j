package org.itineradev.itinera.workflow.checker

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier

/**
 * Whether itinera-kotlin reads the Kotlin class [declaration], read as [type], as data: a data
 * class whose only fields, its superclasses' included, are the `val` properties of its primary
 * constructor, none of a value class, or a `data object` whose fields are all final.
 */
internal fun isData(type: ClassSymbol, declaration: KSClassDeclaration): Boolean =
    when {
        Modifier.DATA !in declaration.modifiers -> false
        declaration.classKind == ClassKind.OBJECT -> isFrozenObject(declaration)
        else -> isDataClass(type, declaration)
    }

private fun isFrozenObject(declaration: KSClassDeclaration): Boolean =
    declaration.getDeclaredProperties().filter(::isStored).all(::isFinal)

private fun isDataClass(type: ClassSymbol, declaration: KSClassDeclaration): Boolean {
    val parameters = declaration.primaryConstructor?.parameters ?: return false
    val fields = declaration.getDeclaredProperties().filter(::isStored)
    return parameters.all(::isComponent) &&
        fields.count() == parameters.size &&
        generateSequence(type.superclass, ClassSymbol::superclass).all(::declaresNoFields)
}

private fun isComponent(parameter: KSValueParameter): Boolean =
    parameter.isVal && !isOfValueClass(parameter)

private fun isOfValueClass(parameter: KSValueParameter): Boolean =
    (parameter.type.resolve().declaration as? KSClassDeclaration)?.let(::isValueClass) ?: false

private fun isValueClass(declaration: KSClassDeclaration): Boolean =
    Modifier.VALUE in declaration.modifiers

private fun declaresNoFields(type: ClassSymbol): Boolean = type.instanceFields.isEmpty()
