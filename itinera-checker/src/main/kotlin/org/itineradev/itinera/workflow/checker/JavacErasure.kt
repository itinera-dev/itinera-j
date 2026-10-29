package org.itineradev.itinera.workflow.checker

import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

/** Names the classes of javac's types as Java reflection names them, erased. */
internal class JavacErasure(private val symbols: JavacSymbols) {
    /** The erased class of [type], named as `Class.getName` names it. */
    fun nameOf(type: TypeMirror): String {
        val erased = symbols.types.erasure(type)
        return when (erased.kind) {
            TypeKind.DECLARED -> binaryNameOf(erased as DeclaredType)
            TypeKind.ARRAY -> "[" + descriptorOf((erased as ArrayType).componentType)
            else -> if (erased.kind.isPrimitive) erased.kind.name.lowercase() else Names.OBJECT
        }
    }

    /** The erased class of [type], named as `Class.getSimpleName` names it. */
    fun simpleNameOf(type: TypeMirror): String {
        val erased = symbols.types.erasure(type)
        return when (erased.kind) {
            TypeKind.DECLARED -> (erased as DeclaredType).asElement().simpleName.toString()
            TypeKind.ARRAY -> simpleNameOf((erased as ArrayType).componentType) + "[]"
            else -> erased.kind.name.lowercase()
        }
    }

    /** The erased class of [type], if it is a class rather than a primitive type or an array. */
    fun classOf(type: TypeMirror): ClassSymbol? {
        val erased = symbols.types.erasure(type)
        return if (erased.kind == TypeKind.DECLARED)
            symbols.classOf((erased as DeclaredType).asElement() as TypeElement)
        else null
    }

    /**
     * The class Java reflection reads for [type] as a type argument, by its binary name: the class
     * of a class or of an array whose class is known, the class of `Object[]` for an array of a
     * type variable or of a parameterized type, and `null` for a type variable and a wildcard.
     */
    fun argumentOf(type: TypeMirror): String? =
        when (type.kind) {
            TypeKind.DECLARED -> binaryNameOf(type as DeclaredType)
            TypeKind.ARRAY -> if (isReifiable(type)) nameOf(type) else OBJECTS
            else -> null
        }

    private fun descriptorOf(component: TypeMirror): String =
        when (component.kind) {
            TypeKind.DECLARED -> "L${binaryNameOf(component as DeclaredType)};"
            TypeKind.ARRAY -> "[" + descriptorOf((component as ArrayType).componentType)
            else -> primitiveDescriptors.getValue(component.kind)
        }

    private fun binaryNameOf(type: DeclaredType): String =
        symbols.elements.getBinaryName(type.asElement() as TypeElement).toString()
}

private const val OBJECTS = "[Ljava.lang.Object;"

private val primitiveDescriptors =
    mapOf(
        TypeKind.BOOLEAN to "Z",
        TypeKind.BYTE to "B",
        TypeKind.CHAR to "C",
        TypeKind.SHORT to "S",
        TypeKind.INT to "I",
        TypeKind.LONG to "J",
        TypeKind.FLOAT to "F",
        TypeKind.DOUBLE to "D",
    )

/**
 * Whether the class of [type] is known on the JVM, as that of an array of type variables is not.
 */
private fun isReifiable(type: TypeMirror): Boolean =
    when (type.kind) {
        TypeKind.ARRAY -> isReifiable((type as ArrayType).componentType)
        TypeKind.DECLARED -> (type as DeclaredType).typeArguments.isEmpty()
        else -> type.kind.isPrimitive
    }
