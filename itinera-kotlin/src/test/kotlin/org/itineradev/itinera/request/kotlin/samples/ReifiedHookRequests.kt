package org.itineradev.itinera.request.kotlin.samples

import org.itineradev.itinera.policy.OnSuccess
import org.itineradev.itinera.policy.PolicyFactory
import org.itineradev.itinera.policy.Role
import org.itineradev.itinera.policy.StepPolicyDescriptor
import org.itineradev.itinera.policy.SyncHook
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.request.kotlin.fromStep
import org.itineradev.itinera.request.kotlin.fromWorkflow
import org.itineradev.itinera.request.kotlin.optionalFromStep
import org.itineradev.itinera.request.kotlin.optionalFromWorkflow
import org.itineradev.itinera.request.kotlin.role
import org.itineradev.itinera.workflow.Sync

/** A role the store provides: putting a parcel on a shelf. */
@Role
fun interface Shelves {
    fun shelve(parcel: String)
}

/** Shelves what each step packed. */
class Shelving

fun reifiedHookRequests(): StepPolicyDescriptor<Shelving, Sync> {
    val parcels = Requests.fromStep<List<String>>("parcels")
    val label = Requests.optionalFromStep<String>("label")
    val aisle = Requests.fromWorkflow<String>("aisle")
    val note = Requests.optionalFromWorkflow<String>("note")
    val shelves = Requests.role<Shelves>()
    return StepPolicyDescriptor.builder("shelving", PolicyFactory(::Shelving))
        .onStepSuccess(
            listOf(parcels, label, aisle, note, shelves),
            SyncHook { _, got ->
                val shelf =
                    "${got[aisle]}: ${got[label] ?: "unlabelled"}, ${got[note] ?: "no note"}"
                val store = got[shelves]
                for (parcel in got[parcels]) store.shelve("$parcel in $shelf")
                OnSuccess.keepDefault()
            },
        )
        .build()
}
