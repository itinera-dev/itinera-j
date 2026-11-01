package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.AdapterAnswer
import org.itineradev.itinera.conformance.model.AdapterModel
import org.itineradev.itinera.conformance.model.AdapterRequest
import org.itineradev.itinera.conformance.model.ValueType
import org.itineradev.itinera.journey.DataBagAccess
import org.itineradev.itinera.request.Got
import org.itineradev.itinera.request.InputAdapterKind
import org.itineradev.itinera.request.Request
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.InputAdapterBuilder
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.Supplied

/**
 * What the scripted [adapter] does when called: it learns the input and the step it is called for,
 * and supplies what its script says for them.
 */
internal class Adapting(private val adapter: AdapterModel) {
    private val key = Requests.key()
    private val step = Requests.stepName()
    private val access =
        Requests.dataBagAccess().takeIf { AdapterRequest.DataBag in adapter.requests }
    private val fromWorkflow =
        adapter.requests.filterIsInstance<AdapterRequest.FromWorkflow>().map(::requestOf)
    private var kept: DataBagAccess? = null

    /**
     * Declares what the adapter requests on [builder]: the input and the step it is called for, its
     * access to the data bag when scripted, and data from the workflow in the order written.
     */
    fun <W, M : Mode> declaredOn(builder: InputAdapterBuilder<W, M>): InputAdapterBuilder<W, M> {
        builder.request(key).request(step)
        access?.let(builder::request)
        fromWorkflow.forEach(builder::request)
        return builder
    }

    /** What the adapter supplies for one call, given [got]. */
    fun supplied(got: Got<InputAdapterKind>): Supplied {
        val calledFor = got[step].toString()
        val given = access?.let(got::get)
        if (calledFor == adapter.keepsAccessOf) kept = given
        return when (val answer = adapter.answerFor(calledFor, got[key])) {
            is AdapterAnswer.Returns -> Supplied.Value(answer.value)
            AdapterAnswer.Nothing -> Supplied.FromDataBag
            is AdapterAnswer.Fails -> throw IllegalStateException(answer.message)
            AdapterAnswer.NonValue -> Supplied.Value(NotAValue)
            AdapterAnswer.ItsStepName -> Supplied.Value(calledFor)
            is AdapterAnswer.Reads -> readThrough(if (answer.kept) kept else given, answer)
        }
    }
}

private fun requestOf(request: AdapterRequest.FromWorkflow): Request<InputAdapterKind, *> =
    Requests.fromWorkflow(request.key, TypeToken.of(request.type.type))

private fun readThrough(access: DataBagAccess?, answer: AdapterAnswer.Reads): Supplied =
    access?.let { through -> found(through, answer.key, answer.type) }?.let(Supplied::Value)
        ?: Supplied.FromDataBag

private fun found(access: DataBagAccess, key: String, type: ValueType): Any? =
    access.read(key, TypeToken.of(type.type)).orNull()
