package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Given
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.IdGenerator
import org.itineradev.itinera.conformance.model.value
import org.itineradev.itinera.conformance.run.NotAValue

/**
 * The sentences that declare what the workflow instance is created with: its initial data, where
 * its journey ID comes from, and its initial global metadata.
 */
internal class DataSentences(private val world: World) {
    private val model
        get() = world.model

    @Given("the data bag is empty")
    fun theDataBagIsEmpty() {
        model.emptyDataBag()
    }

    @Given("the data bag contains:")
    fun theDataBagContains(data: DataTable) {
        for (row in data.asMaps()) model.contains(cell(row, "key"), value(cell(row, "value")))
    }

    @Given("the data bag contains a non-value as {string}")
    fun theDataBagContainsANonValue(key: String) {
        model.contains(key, NotAValue)
    }

    @Given("the workflow has no ID generator")
    fun noIdGenerator() {
        model.generateIdsWith(IdGenerator.None)
    }

    @Given("the workflow's ID generator returns {string}")
    fun idGeneratorReturns(id: String) {
        model.generateIdsWith(IdGenerator.Returns(id))
    }

    @Given("the workflow instance is created with the global metadata:")
    fun createdWithTheGlobalMetadata(metadata: DataTable) {
        for (row in metadata.asMaps()) {
            model.globalMetadata(cell(row, "key"), value(cell(row, "value")))
        }
    }
}
