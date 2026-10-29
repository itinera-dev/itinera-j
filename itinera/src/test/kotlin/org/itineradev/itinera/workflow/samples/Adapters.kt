package org.itineradev.itinera.workflow.samples

import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.InputAdapterDescriptor
import org.itineradev.itinera.workflow.Supplied
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.Violation
import org.itineradev.itinera.workflow.Violations
import org.itineradev.itinera.workflow.WorkflowDescriptor

private val LONG = TypeToken.of(Long::class.javaObjectType)
private val KEY = Requests.key()
private val PRICE = Requests.fromWorkflow("price", LONG)
private val ACCESS = Requests.dataBagAccess()

/** Supplies the amount, the price less any discount, and leaves every other input to the bag. */
private fun pricing(got: Got<InputAdapterKind>): Supplied =
    if (got[KEY] == "amount")
        Supplied.Value(got[PRICE] - (got[ACCESS].read("discount", LONG).orNull() ?: 0L))
    else Supplied.FromDataBag

fun adapters(): WorkflowDescriptor<Orders, Sync> {
    val prices =
        InputAdapterDescriptor.builder<Orders>("pricing", "charge") { _, got -> pricing(got) }
            .request(KEY)
            .request(PRICE)
            .request(ACCESS)
            .build()
    return WorkflowDescriptor.builder(Orders::class.java, "orders")
        .step(steps())
        .inputAdapter(prices)
        .build()
}

fun misadapted(): List<Violation> {
    val pricing =
        InputAdapterDescriptor.builder<Orders>("pricing", "charge") { _, _ -> Supplied.FromDataBag }
            .step("ship")
            .build()
    return try {
        WorkflowDescriptor.builder(Orders::class.java, "orders")
            .step(steps())
            .inputAdapter(pricing)
            .inputAdapter(pricing)
            .build()
        emptyList()
    } catch (refused: Violations) {
        refused.violations
    }
}
