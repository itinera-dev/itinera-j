package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.HookRequest
import org.itineradev.itinera.conformance.model.ModelException

private val factColumns: Map<String, (HookRequest) -> Boolean> =
    mapOf(
        "attempt number" to ::isAttempt,
        "step name" to ::isStepName,
        "journey ID" to ::isJourneyId,
        "cause" to ::isCause,
        "error" to ::isError,
        "failure reason" to ::isReason,
    )

private val receivedColumns = setOf("policy", "hook", "calls") + factColumns.keys

/** The table that checks what each hook received, one row for each hook. */
internal class HooksReceivedSentences(
    private val world: World,
    private val called: HookCalledSentences,
    private val facts: HookReceivedFactSentences,
) {
    @Then("the hooks received:")
    fun theHooksReceived(table: DataTable) {
        table.asMaps().forEach(::hookReceived)
    }

    private fun hookReceived(row: Map<String?, String?>) {
        only(receivedColumns, row)
        val hook = cell(row, "hook")
        val policy = cell(row, "policy")
        val calls = row["calls"]
        if (calls != null) called.wasCalled(hook, policy, count(calls))
        for ((column, asks) in factColumns) {
            val written = row[column]
            if (written == "none") world.received(hook, policy, asks, null)
            else if (written != null) fact(hook, policy, column, written)
        }
    }

    private fun fact(hook: String, policy: String, column: String, written: String) {
        when (column) {
            "attempt number" -> facts.receivedTheAttempt(hook, policy, count(written))
            "step name" -> facts.receivedTheStepName(hook, policy, written)
            "journey ID" -> facts.receivedTheJourneyId(hook, policy, written)
            "cause" -> facts.receivedTheCause(hook, policy, written)
            "error" -> facts.receivedTheErrorWithTheMessage(hook, policy, written)
            else -> throw ModelException("the column $column holds only none, not $written")
        }
    }
}
