package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.AdapterAnswer
import org.itineradev.itinera.conformance.model.AdapterRequest
import org.itineradev.itinera.conformance.model.ValueType
import org.itineradev.itinera.conformance.model.value

/** The sentences that script what each input adapter requests and supplies. */
internal class AdapterSentences(private val world: World) {
    private fun adapter(name: String) = world.model.workflow.adapter(name)

    @Given(
        """^the input adapter "([^"]*)" returns """ +
            """(?!nothing for |a non-value for |no result for |a throwing record for |"[^"]*" of type )""" +
            """(.+) for "([^"]*)"$"""
    )
    fun returns(adapter: String, written: String, key: String) {
        adapter(adapter).answers(key, AdapterAnswer.Returns(value(written)))
    }

    @Given("the input adapter {string} returns nothing for {string}")
    fun returnsNothing(adapter: String, key: String) {
        adapter(adapter).answers(key, AdapterAnswer.Nothing)
    }

    @Given("the input adapter {string} fails with {string} for {string}")
    fun failsWith(adapter: String, message: String, key: String) {
        adapter(adapter).answers(key, AdapterAnswer.Fails(message))
    }

    @Given("the input adapter {string} returns a non-value for {string}")
    fun returnsANonValue(adapter: String, key: String) {
        adapter(adapter).answers(key, AdapterAnswer.NonValue)
    }

    @Given("the input adapter {string} requests data from the workflow {string} of type {word}")
    fun requestsDataFromTheWorkflow(adapter: String, key: String, type: String) {
        adapter(adapter).requests += AdapterRequest.FromWorkflow(key, ValueType.named(type))
    }

    @Given("the input adapter {string} requests the step name and returns it for {string}")
    fun returnsTheStepName(adapter: String, key: String) {
        adapter(adapter).answers(key, AdapterAnswer.ItsStepName)
    }

    @Given("the input adapter {string} requests the data bag")
    fun requestsTheDataBag(adapter: String) {
        adapter(adapter).requests += AdapterRequest.DataBag
    }

    @Given(
        "the input adapter {string} returns {string} of type {word}, read from the data bag, for {string}"
    )
    fun returnsWhatItReads(adapter: String, key: String, type: String, input: String) {
        adapter(adapter).answers(input, AdapterAnswer.Reads(key, ValueType.named(type)))
    }

    @Given(
        "the input adapter {string} keeps its data bag access when called for step {string}, " +
            "and returns {string} of type {word}, read through that kept access, " +
            "for {string} when called for step {string}"
    )
    fun readsThroughKeptAccess(
        adapter: String,
        kept: String,
        key: String,
        type: String,
        input: String,
        step: String,
    ) {
        val scripted = adapter(adapter)
        scripted.keepsAccessOf(kept)
        scripted.answers(input, AdapterAnswer.Reads(key, ValueType.named(type), kept = true), step)
    }
}
