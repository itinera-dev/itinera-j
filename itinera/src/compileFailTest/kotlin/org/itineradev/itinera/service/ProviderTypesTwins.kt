package org.itineradev.itinera.service

import org.itineradev.itinera.request.AttemptProviderKind
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.JourneyProviderKind
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowBuilder

/** What [ProviderTypesTest] refuses, written so that it compiles. */
internal object ProviderTypesTwins {
    class Shop

    fun inJourney(got: Got<JourneyProviderKind<Sync>>) =
        listOf(
            got[Requests.journeyId()],
            got[Requests.fromWorkflow("region", TypeToken.of(String::class.java))],
            got[Requests.providerReporter()],
        )

    fun inAttempt(got: Got<AttemptProviderKind<Sync>>) =
        listOf(got[Requests.stepName()], got[Requests.attempt()], got[Requests.journeyId()])

    val attempted =
        ProviderDescriptor.perAttempt<Shop, String>("p", "k", String::class.java) { _, _ ->
                Provided.NoInstance
            }
            .request(Requests.attempt())

    fun provide(workflow: WorkflowBuilder<Shop, Sync>, provider: ProviderDescriptor<Any?, Sync>) =
        workflow.provider(provider)
}
