package org.itineradev.itinera.engine

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Reads Kotlin classes, which plain Java reflection cannot: `itinera-kotlin` provides it through
 * `java.util.ServiceLoader`, and without it no Kotlin class is data, and no annotated Kotlin class
 * is read.
 *
 * Part of itinera's engine, not of its API.
 */
public interface KotlinClasses {
    /** What a Kotlin class is as data. */
    public fun shapeOf(type: Class<*>): KotlinShape

    /** What a method of a Kotlin class declares that Java reflection cannot read. */
    public fun functionOf(method: Method): KotlinFunction
}

/** What a method of a Kotlin class declares that Java reflection cannot read. */
public class KotlinFunction(
    /** Whether it is a `suspend` function. */
    public val isSuspend: Boolean,
    /**
     * The class of its declared return type, which Java reflection reads as `Object` for a
     * `suspend` function.
     */
    public val returnType: Class<*>,
    /** Whether its return type admits `null`, such as `Outcome?`. */
    public val returnsNullable: Boolean,
) {
    /** Reads functions. */
    public companion object {
        /**
         * [method] as Java reflection reads it: never `suspend`, and returning its return type,
         * which is taken not to admit `null`.
         */
        public fun ofJava(method: Method): KotlinFunction =
            KotlinFunction(
                isSuspend = false,
                returnType = method.returnType,
                returnsNullable = false,
            )
    }
}

/** What a Kotlin class is as data. */
public sealed interface KotlinShape

/**
 * A data class whose only fields, its superclasses' included, are the `val` properties of its
 * primary constructor, which rebuilds it.
 */
public class DataClass(
    /** The properties of the primary constructor, in its order. */
    public val components: List<Component>,
    /** The primary constructor. */
    public val constructor: Constructor<*>,
) : KotlinShape

/** A `data object`, whose fields are all final: its one instance is shared, never copied. */
public class DataObject(
    /** Its fields. */
    public val components: List<Component>
) : KotlinShape

/** A Kotlin class that is not data: anything else, a value class included. */
public data object NotData : KotlinShape

/** One part of a record, a data class or a `data object`: its field and its declared type. */
public class Component(
    /** The field that holds it. */
    public val field: Field,
    /** Its declared type, with whether `null` is of it. */
    public val type: ValueType,
)
