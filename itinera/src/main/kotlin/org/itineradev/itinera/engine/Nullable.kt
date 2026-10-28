package org.itineradev.itinera.engine

/**
 * What custom code returned, as what it is: possibly `null` whatever its declared type, since
 * Kotlin does not check what Java code returns.
 *
 * Part of itinera's engine, not of its API.
 */
public fun <T : Any> nullable(returned: T?): T? = returned
