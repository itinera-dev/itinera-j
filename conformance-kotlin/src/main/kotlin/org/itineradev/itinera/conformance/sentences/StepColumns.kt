package org.itineradev.itinera.conformance.sentences

import org.itineradev.itinera.conformance.model.Ending
import org.itineradev.itinera.conformance.model.Input
import org.itineradev.itinera.conformance.model.Model
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.RequestedService
import org.itineradev.itinera.conformance.model.ScriptedAttempt
import org.itineradev.itinera.conformance.model.ServiceType
import org.itineradev.itinera.conformance.model.ValueType

private val stepColumns =
    setOf("step", "policies", "inputs", "services", "outcome", "code", "rescue")

/**
 * Scripts the step of one row of the workflow's steps table with what its filled cells stand for:
 * its policies, its inputs and services, how every attempt ends and whether it has a rescue step.
 */
internal fun Model.scriptColumns(row: Map<String?, String?>) {
    only(stepColumns, row)
    val step = cell(row, "step")
    val script = step(step)
    val policies = row["policies"]
    val inputs = row["inputs"]
    val services = row["services"]
    val outcome = row["outcome"]
    val rescue = row["rescue"]
    if (policies != null) script.attach(step, listed(policies))
    if (inputs != null) script.inputs += listed(inputs).map(::inputOf)
    if (services != null) script.services += listed(services).map(::serviceOf)
    if (outcome != null) script.attempts(step, listOf(everyAttempt(outcome, row)))
    if (row["code"] != null && outcome != Ending.FAILURE.written) {
        throw ModelException("the step \"$step\" has a code but no failure")
    }
    if (rescue != null && isYes(rescue)) rescue(step)
}

/** The items of a cell, separated by commas. */
private fun listed(written: String): List<String> = written.split(",").map(String::trim)

private fun inputOf(written: String): Input {
    val (key, type) = keyed(written)
    return Input(key, ValueType.named(type), optional = false)
}

private fun serviceOf(written: String): RequestedService {
    val (key, type) = keyed(written)
    return RequestedService(key, ServiceType.named(type))
}

/** A request written `key: type`. */
private fun keyed(written: String): Pair<String, String> {
    val halves = written.split(":").map(String::trim)
    if (halves.size != 2) throw ModelException("$written is not written as key: type")
    return halves[0] to halves[1]
}

private fun everyAttempt(outcome: String, row: Map<String?, String?>): ScriptedAttempt =
    when (val ending = Ending.named(outcome)) {
        Ending.SUCCESS -> ScriptedAttempt(1, ending)
        Ending.FAILURE -> ScriptedAttempt(1, ending, cell(row, "code"))
        Ending.SKIPPED,
        Ending.ERROR -> throw ModelException("$outcome is not an outcome of the steps table")
    }
