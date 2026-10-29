package org.itineradev.itinera.request.kotlin.samples

import org.itineradev.itinera.request.BuildKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.kotlin.input
import org.itineradev.itinera.request.kotlin.optionalInput
import org.itineradev.itinera.request.kotlin.service

/** Holds parcels until they ship. */
fun interface Store {
    fun hold(parcel: String)
}

fun reifiedRequests(): List<Request<BuildKind, *>> =
    listOf(
        Requests.input<List<String>>("parcels"),
        Requests.optionalInput<String>("note"),
        Requests.service<Store>("store"),
    )
