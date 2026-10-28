package org.itineradev.itinera.engine

import java.util.ServiceLoader

/**
 * Java's own forms of requests, which `itinera-java` provides through `java.util.ServiceLoader`:
 * without it, a parameter of type `Optional` requests nothing a value can be.
 *
 * Part of itinera's engine, not of its API.
 */
public interface JavaForms {
    /**
     * The answer to an optional request written as `Optional<T>`: an `Optional` of what was
     * resolved.
     */
    public val optional: Answer
}

/** Java's own forms of requests, if `itinera-java` is there. */
internal val javaForms: JavaForms? =
    ServiceLoader.load(JavaForms::class.java, JavaForms::class.java.classLoader).firstOrNull()
