package org.itineradev.itinera.value

import java.lang.reflect.AnnotatedParameterizedType
import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.engine.ClassType
import org.itineradev.itinera.engine.ValueType

/**
 * The type a request or a read of the data bag wants, generics included, with whether `null` is of
 * it, at the top and for each type argument. The value read is checked against it, since the JVM
 * erases type arguments, and read only when it is of that type: nothing converts, so a stored `Int`
 * is not a `Long`.
 *
 * A token is made once and used to declare a request and to read its value:
 * - [of] makes the token of a class, refusing `null`, and [ofNullable] one admitting it; a class
 *   that is generic is read raw, with any elements, values or components;
 * - in Java, an anonymous subclass names a generic type, as `new TypeToken<List<Long>>() {}`. Its
 *   type arguments admit `null` unless JSpecify's annotations say otherwise: within `@NullMarked`
 *   code, a type not marked `@Nullable` refuses it, at the top too;
 * - in Kotlin, `typeToken<List<Long?>>()` from `itinera-kotlin` keeps Kotlin's nullability.
 *
 * A token never holds a primitive: the token of `long` is the token of `Long`. Making a token of a
 * type no value can be, such as an array, a `LinkedList` or a map whose keys are not strings,
 * throws [IllegalArgumentException], saying why.
 *
 * @param T the type the value is read as.
 * @sample org.itineradev.itinera.value.samples.typeTokens
 */
@ExperimentalItineraApi
public abstract class TypeToken<T> {
    internal val type: ValueType

    /**
     * Reads the type this anonymous subclass gives as its type argument, with what JSpecify's
     * annotations say of `null` in it. Kotlin writes no such annotations, so from Kotlin every
     * level of the type admits `null`; `typeToken` from `itinera-kotlin` keeps Kotlin's
     * nullability.
     *
     * @throws IllegalArgumentException when no value can be of that type, or it is not a class or a
     *   parameterised type, such as a type variable.
     * @sample org.itineradev.itinera.value.samples.genericTypeTokens
     */
    protected constructor() {
        val supertype = javaClass.annotatedSuperclass
        require(
            supertype is AnnotatedParameterizedType && javaClass.superclass == TypeToken::class.java
        ) {
            "A type token's anonymous subclass gives its type as TypeToken's type argument, " +
                "as new TypeToken<List<Long>>() {}."
        }
        type =
            requestable(
                declaredType(
                    supertype.annotatedActualTypeArguments.single(),
                    isNullMarked(javaClass),
                )
            )
    }

    internal constructor(type: ValueType) {
        this.type = requestable(type)
    }

    /**
     * Whether the other is a token of the same type, with the same nullability throughout.
     *
     * @sample org.itineradev.itinera.value.samples.typeTokens
     */
    override fun equals(other: Any?): Boolean = other is TypeToken<*> && other.type == type

    /**
     * A hash of the type, the same for equal tokens.
     *
     * @sample org.itineradev.itinera.value.samples.typeTokens
     */
    override fun hashCode(): Int = type.hashCode()

    /**
     * The type, with `?` after each part `null` is of, such as `List<Long?>`.
     *
     * @sample org.itineradev.itinera.value.samples.typeTokens
     */
    override fun toString(): String = type.toString()

    /** Makes type tokens. */
    public companion object {
        /**
         * The token of a class, refusing `null`; a primitive class gives its wrapper's.
         *
         * @throws IllegalArgumentException when no value can be an instance of the class.
         * @sample org.itineradev.itinera.value.samples.typeTokens
         */
        @JvmStatic
        public fun <T : Any> of(type: Class<T>): TypeToken<T> = Made(classType(type, false))

        /**
         * The token of a class, admitting `null`.
         *
         * @throws IllegalArgumentException when no value can be an instance of the class.
         * @sample org.itineradev.itinera.value.samples.typeTokens
         */
        @JvmStatic
        public fun <T : Any> ofNullable(type: Class<T>): TypeToken<T?> = Made(classType(type, true))

        private fun classType(type: Class<*>, nullable: Boolean): ClassType =
            ClassType(boxed(type), emptyList(), nullable)
    }
}

/** A token made from its type rather than read from an anonymous subclass. */
internal class Made<T>(type: ValueType) : TypeToken<T>(type)
