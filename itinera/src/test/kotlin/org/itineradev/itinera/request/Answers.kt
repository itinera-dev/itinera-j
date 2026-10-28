@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.request

import org.itineradev.itinera.ExperimentalItineraApi

/** Answers each request it holds with its value, as the executor will once it resolves them. */
class Answers<K>(private val answers: Map<Request<*, *>, Any?>) : Got<K> {
    @Suppress("UNCHECKED_CAST")
    override fun <T> get(request: Request<K, T>): T = answers.getValue(request) as T
}
