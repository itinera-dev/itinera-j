package org.itineradev.itinera.engine

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass

/**
 * A copy of [error] that one receiver owns, so nothing it changes in it, such as its stack trace or
 * its suppressed exceptions, reaches anyone else: [error] written by Java serialisation and read
 * back, with its class, message, stack trace, cause, suppressed exceptions and fields. It is
 * [error] itself when it cannot be copied so, such as an exception holding a field that is not
 * serialisable. An [Error] thrown while copying is never caught.
 */
internal fun ownCopyOf(error: Throwable): Throwable = runCatching {
    copied(error)
}
    .onFailure(::rethrowError)
    .getOrDefault(error)

private fun copied(error: Throwable): Throwable {
    val written = ByteArrayOutputStream()
    ObjectOutputStream(written).use { output -> output.writeObject(error) }
    val read = ByteArrayInputStream(written.toByteArray())
    return ReadingBack(read, error.javaClass.classLoader).use(ObjectInputStream::readObject)
        as Throwable
}

/** Reads back what was written, finding classes through [loader], the error's own. */
private class ReadingBack(input: InputStream, private val loader: ClassLoader?) :
    ObjectInputStream(input) {
    override fun resolveClass(description: ObjectStreamClass): Class<*> =
        try {
            Class.forName(description.name, false, loader)
        } catch (_: ClassNotFoundException) {
            super.resolveClass(description)
        }
}
