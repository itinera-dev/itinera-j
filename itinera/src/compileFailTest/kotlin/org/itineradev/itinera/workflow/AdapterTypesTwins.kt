package org.itineradev.itinera.workflow

import org.itineradev.itinera.journey.DataBagAccess
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.value.TypeToken

/** What [AdapterTypesTest] refuses, written so that it compiles. */
internal object AdapterTypesTwins {
    class Shop

    fun read(got: Got<InputAdapterKind>) =
        listOf(
            got[Requests.key()],
            got[Requests.stepName()],
            got[Requests.journeyId()],
            got[Requests.dataBagAccess()],
        )

    fun read(access: DataBagAccess) =
        access.read("amount", TypeToken.of(Long::class.javaObjectType))

    fun adapt(workflow: WorkflowBuilder<Shop, Sync>, adapter: InputAdapterDescriptor<Any?, Sync>) =
        workflow.inputAdapter(adapter)
}
