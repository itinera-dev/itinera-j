package org.itineradev.itinera.value

import java.lang.reflect.Constructor
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.ServiceLoader
import java.util.UUID
import org.itineradev.itinera.engine.Component
import org.itineradev.itinera.engine.DataClass
import org.itineradev.itinera.engine.DataObject
import org.itineradev.itinera.engine.KotlinClasses
import org.itineradev.itinera.engine.NotData

/** What the instances of a class are as values, which decides how they are checked and copied. */
internal sealed interface Shape {
    /** Immutable, so shared: a string, a boolean, an integer, a JDK value type. */
    data object Shared : Shape

    /** A `Float` or a `Double`, a value only when finite. */
    data object Floating : Shape

    /** A list, copied into an `ArrayList`. */
    data object Sequence : Shape

    /** A set, copied into a `LinkedHashSet`. */
    data object Distinct : Shape

    /** A map, whose keys must be strings, copied into a `LinkedHashMap`. */
    data object Keyed : Shape

    /** A record or a data class, rebuilt through its constructor from its copied components. */
    class Composite(val components: List<Component>, val constructor: Constructor<*>) : Shape

    /**
     * An enum constant or a `data object`, shared once its fields are checked to hold immutable
     * values.
     */
    class Singleton(val components: List<Component>) : Shape

    /** Not a value. */
    data object None : Shape
}

/**
 * The classes whose instances are immutable values, matched exactly, since some can be extended.
 */
private val sharedClasses: Set<Class<*>> =
    setOf(
        Boolean::class.javaObjectType,
        Char::class.javaObjectType,
        String::class.java,
        Byte::class.javaObjectType,
        Short::class.javaObjectType,
        Int::class.javaObjectType,
        Long::class.javaObjectType,
        BigInteger::class.java,
        BigDecimal::class.java,
        UUID::class.java,
        URI::class.java,
        Instant::class.java,
        LocalDate::class.java,
        LocalTime::class.java,
        LocalDateTime::class.java,
        OffsetDateTime::class.java,
        OffsetTime::class.java,
        ZonedDateTime::class.java,
        Duration::class.java,
        Period::class.java,
        Year::class.java,
        YearMonth::class.java,
        MonthDay::class.java,
    )

private val floatingClasses: Set<Class<*>> =
    setOf(Float::class.javaObjectType, Double::class.javaObjectType)

/** The reader of Kotlin classes that `itinera-kotlin` provides, if it is there. */
internal val kotlinClasses: KotlinClasses? =
    ServiceLoader.load(KotlinClasses::class.java, KotlinClasses::class.java.classLoader)
        .firstOrNull()

private val shapes =
    object : ClassValue<Shape>() {
        override fun computeValue(type: Class<*>): Shape = shapeOf(type)
    }

/** What instances of a class are as values. */
internal fun shape(type: Class<*>): Shape = shapes.get(type)

private fun shapeOf(type: Class<*>): Shape =
    when {
        type in sharedClasses || isA<ZoneId>(type) -> Shape.Shared
        type in floatingClasses -> Shape.Floating
        isA<Enum<*>>(type) -> enumShape(type)
        isA<List<*>>(type) -> Shape.Sequence
        isA<Set<*>>(type) -> Shape.Distinct
        isA<Map<*, *>>(type) -> Shape.Keyed
        isKotlin(type) -> kotlinShape(type)
        type.isRecord -> recordShape(type)
        else -> Shape.None
    }

internal inline fun <reified T> isA(type: Class<*>): Boolean = T::class.java.isAssignableFrom(type)

/** Whether a class was compiled by Kotlin, which marks every class it compiles. */
internal fun isKotlin(type: Class<*>): Boolean = type.isAnnotationPresent(Metadata::class.java)

private fun kotlinShape(type: Class<*>): Shape =
    when (val read = kotlinClasses?.shapeOf(type)) {
        is DataClass -> composite(read.components, read.constructor)
        is DataObject -> singleton(read.components)
        NotData -> Shape.None
        null -> if (type.isRecord) recordShape(type) else Shape.None
    }

/**
 * A record or data class, when itinera may read its fields and call its constructor: a class in a
 * module that does not open its package to itinera is not a value.
 */
internal fun composite(components: List<Component>, constructor: Constructor<*>): Shape =
    if (components.all(::opened) && constructor.trySetAccessible())
        Shape.Composite(components, constructor)
    else Shape.None

/** An enum constant or `data object`, when itinera may read its fields. */
internal fun singleton(components: List<Component>): Shape =
    if (components.all(::opened)) Shape.Singleton(components) else Shape.None

/** Makes a component's field readable by itinera, saying whether its module allowed it. */
private fun opened(component: Component): Boolean = component.field.trySetAccessible()
