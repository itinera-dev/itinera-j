package org.itineradev.itinera.workflow.checker

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter

/** Expands type aliases as Kotlin's reflection does, which never sees them. */
internal class KspAliases(private val resolver: Resolver) {
    /**
     * [type], or, when it names a type alias, the type the alias stands for, with the alias's
     * arguments in place of its type parameters, expanded again while it names one.
     */
    fun expanded(type: KSType): KSType {
        val alias = type.declaration as? KSTypeAlias ?: return type
        val bound = alias.typeParameters.map(::nameOf).zip(type.arguments).toMap()
        val target = alias.type.resolve()
        val substituted =
            target.replace(target.arguments.map { argument -> substituted(argument, bound) })
        return expanded(if (type.isMarkedNullable) substituted.makeNullable() else substituted)
    }

    private fun substituted(
        argument: KSTypeArgument,
        bound: Map<String, KSTypeArgument>,
    ): KSTypeArgument {
        val type = argument.type?.resolve() ?: return argument
        val parameter = type.declaration as? KSTypeParameter
        return when {
            parameter != null ->
                bound[nameOf(parameter)]?.let { given -> asUsed(given, type) } ?: argument
            type.arguments.isEmpty() -> argument
            else ->
                argumentOf(
                    type.replace(type.arguments.map { inner -> substituted(inner, bound) }),
                    argument,
                )
        }
    }

    /** [given], made nullable when the alias uses its parameter as nullable, as `T?`. */
    private fun asUsed(given: KSTypeArgument, use: KSType): KSTypeArgument {
        val type = given.type?.resolve()
        return if (use.isMarkedNullable && type != null) argumentOf(type.makeNullable(), given)
        else given
    }

    private fun argumentOf(type: KSType, projected: KSTypeArgument): KSTypeArgument =
        resolver.getTypeArgument(resolver.createKSTypeReferenceFromKSType(type), projected.variance)
}

/** The name of the type parameter [parameter]. */
internal fun nameOf(parameter: KSTypeParameter): String = parameter.name.asString()
