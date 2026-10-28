package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.run.Admission
import org.itineradev.itinera.conformance.run.Ended
import org.itineradev.itinera.conformance.run.Unmet
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.executor.Refused
import org.itineradev.itinera.workflow.Violation

/** The sentences that check how the workflow was admitted. */
internal class AdmissionSentences(private val world: World) {
    @Then("admission is refused with the violations:")
    fun admissionIsRefused(table: DataTable) {
        val expected = table.asMaps().map(::violationOf).sorted()
        val found = refusedWith().map(::nameOf).sorted()
        holds(found == expected, "the violations $expected", "$found")
    }

    /** The violations the builder refused the workflow with, or `run` refused its instance with. */
    private fun refusedWith(): List<Violation> {
        val admission = world.admission
        if (admission is Admission.Refused) return admission.violations.violations
        val refused = (world.journeys.lastOrNull()?.ended as? Ended.Refused)?.refusal?.refused
        if (refused is Refused.Inadmissible) return refused.violations
        throw Unmet("expected admission to be refused; it was admitted")
    }
}

private fun violationOf(row: Map<String?, String?>): String = cell(row, "violation")

private fun nameOf(violation: Violation): String = violation.named.toString()
