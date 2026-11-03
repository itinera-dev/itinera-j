package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.workflow.ListedService
import org.itineradev.itinera.workflow.ListedStep

/** The sentences that list the workflow and check its listing. */
internal class ListingSentences(private val world: World) {
    @When("the workflow is listed")
    fun isListed() {
        world.admit()
    }

    /** Compares only the columns the table has, each cell as the cases write it. */
    @Then("the listing is:")
    fun theListingIs(table: DataTable) {
        val columns = table.cells().first()
        val expected = table.asMaps().map(::written)
        val found = world.listing.map { step -> rowOf(step, columns) }
        holds(found == expected, "the listing $expected", "$found")
    }
}

/** A row as the table writes it, an empty cell as an empty string. */
private fun written(row: Map<String?, String?>): Map<String?, String> = row.mapValues(::orEmpty)

private fun orEmpty(cell: Map.Entry<String?, String?>): String = cell.value.orEmpty()

private fun rowOf(step: ListedStep, columns: List<String?>): Map<String?, String> =
    columns.associateWith { column ->
        cellOf(step, column)
    }

private fun cellOf(step: ListedStep, column: String?): String =
    when (column) {
        "step" -> step.step.toString()
        "position" -> step.position.toString()
        "policies" -> step.policies.joinToString(", ")
        "adapter" -> step.adapter?.toString().orEmpty()
        "services" -> written(step.services)
        "rescue" -> step.rescue?.step?.toString().orEmpty()
        "rescue adapter" -> step.rescue?.adapter?.toString().orEmpty()
        "rescue services" -> step.rescue?.services?.let(::written).orEmpty()
        else -> throw ModelException("a listing has no column $column")
    }

/** Services as the cases write them: `<key>: <provider>`, separated by commas. */
private fun written(services: List<ListedService>): String =
    services.joinToString(", ", transform = ::written)

private fun written(service: ListedService): String = "${service.key}: ${service.provider}"
