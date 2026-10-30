package org.itineradev.itinera.engine

import org.itineradev.itinera.value.Made
import org.itineradev.itinera.value.NotAValueException
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.value.copy
import org.itineradev.itinera.value.isOf

/**
 * Captures values and gives receivers their copies, wherever something enters a journey or is read
 * in it.
 *
 * Part of itinera's engine, not of its API.
 */
public object Values {
    /**
     * Checks an object and copies it into a copy only itinera holds: [Captured], or [NotAValue]
     * when it is not a value, or when copying it throws, which [NotAValue] then carries.
     */
    @JvmStatic
    public fun capture(candidate: Any?): Capture = attempt(candidate).fold(::Captured, ::notAValue)

    /**
     * A receiver's own copy of a captured value, when it is of the type it is read as: [Received],
     * or [WrongType] when it is of another type, or when copying it throws, which [WrongType] then
     * carries.
     */
    @JvmStatic
    public fun <T> receive(stored: Captured, type: TypeToken<T>): Receipt<T> =
        if (!isOf(stored.value, type.type)) WrongType(null)
        else attempt(stored.value).fold(::receipt, ::wrongType)

    /**
     * The token of a type that `itinera-kotlin` read from a Kotlin type.
     *
     * @throws IllegalArgumentException when no value can be of that type.
     */
    @JvmStatic public fun <T> token(type: ValueType): TypeToken<T> = Made(type)

    @Suppress("UNCHECKED_CAST")
    private fun <T> receipt(copy: Any?): Receipt<T> = Received(copy as T)

    /** Copies, catching everything custom code throws but an [Error], which is never caught. */
    private fun attempt(value: Any?): Result<Any?> = runCatching {
        copy(value)
    }
        .onFailure(::rethrowError)

    private fun notAValue(thrown: Throwable): NotAValue = NotAValue(thrownByCustomCode(thrown))

    private fun wrongType(thrown: Throwable): WrongType = WrongType(thrownByCustomCode(thrown))

    private fun thrownByCustomCode(thrown: Throwable): Throwable? = thrown.takeUnless(::isNotAValue)

    private fun isNotAValue(thrown: Throwable): Boolean = thrown is NotAValueException
}

/** What capturing an object found. */
public sealed interface Capture

/** The private copy of a value, which only itinera holds. */
public class Captured
internal constructor(
    /** The copy. */
    public val value: Any?
) : Capture

/** Not a value, with what copying it threw, if it threw. */
public class NotAValue
internal constructor(
    /** What the application's code threw while it was copied, if anything. */
    public val error: Throwable?
) : Capture

/** What a receiver gets when it reads a captured value as a type. */
public sealed interface Receipt<out T>

/** The receiver's own copy, of the type it asked for. */
public class Received<out T>
internal constructor(
    /** The copy. */
    public val value: T
) : Receipt<T>

/** The value is of another type, or copying it threw what this carries. */
public class WrongType
internal constructor(
    /** What the application's code threw while it was copied, if anything. */
    public val error: Throwable?
) : Receipt<Nothing>
