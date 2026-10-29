package org.itineradev.itinera.conformance.sentences

import io.cucumber.datatable.DataTable
import io.cucumber.java.en.Then
import org.itineradev.itinera.conformance.World
import org.itineradev.itinera.conformance.model.Hook
import org.itineradev.itinera.conformance.run.HookCall
import org.itineradev.itinera.conformance.run.holds
import org.itineradev.itinera.conformance.run.holdsOfStream
import org.itineradev.itinera.conformance.trace.lineOf
import org.itineradev.itinera.event.ContributionCommitted

/** The sentences that read which hooks were called, in what order, and what they reached. */
internal class HookCalledSentences(private val world: World) {
    @Then("the hook {string} of policy {string} was called {int} times")
    fun wasCalled(hook: String, policy: String, times: Int) {
        val calls = world.callsOf(hook, policy).size
        holds(calls == times, "\"$hook\" of \"$policy\" called $times times", "$calls calls")
    }

    @Then("the hook {string} of policy {string} was not called")
    fun wasNotCalled(hook: String, policy: String) {
        wasCalled(hook, policy, 0)
    }

    @Then("the hooks were called in this order:")
    fun calledInThisOrder(table: DataTable) {
        val expected = table.asMaps().map(::calledAs)
        val found = world.witness.hooksCalled.map(::calledAs)
        holds(found == expected, "the hooks called in the order $expected", "$found")
    }

    @Then("the operation {string} of the role {string} was called {int} times")
    fun operationWasCalled(operation: String, role: String, times: Int) {
        val calls = world.witness.roleCalls(role, operation)
        holds(calls == times, "\"$operation\" of \"$role\" called $times times", "$calls calls")
    }

    @Then(
        "the contribution of {string} is recorded as made by the hook {string} of policy {string}"
    )
    fun contributionMadeByTheHook(key: String, hook: String, policy: String) {
        val events = world.stream()
        val source = "$policy, $hook"
        holdsOfStream(
            events.filterIsInstance<ContributionCommitted>().any { event ->
                madeBy(event, key, source)
            },
            "\"$key\" committed as made by \"$hook\" of \"$policy\"",
            events,
        )
    }
}

private fun calledAs(row: Map<String?, String?>): Pair<String, Hook> =
    cell(row, "policy") to Hook.named(cell(row, "hook"))

private fun calledAs(call: HookCall): Pair<String, Hook> = call.policy to call.hook

private fun madeBy(event: ContributionCommitted, key: String, source: String): Boolean =
    event.key == key && lineOf(event).cells["source"] == source
