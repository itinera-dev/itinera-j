package org.itineradev.itinera.conformance.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ModelTest {
    private fun orders(): Model = Model().apply { declare("orders", listOf("charge", "ship")) }

    @Test
    fun `a workflow is declared once`() {
        assertThrows<ModelException> { orders().declare("refunds", listOf("refund")) }
    }

    @Test
    fun `a sentence about a step needs the workflow to have it`() {
        assertThrows<ModelException> { orders().step("refund") }
    }

    @Test
    fun `a sentence about the workflow needs one declared`() {
        assertThrows<ModelException> { Model().step("charge") }
    }

    @Test
    fun `two steps of one name share one script`() {
        val model = Model().apply { declare("orders", listOf("charge", "charge")) }
        assertEquals(listOf("charge"), model.workflow.scripts.keys.toList())
    }

    @Test
    fun `a rescue step is scripted under the step's name with _rescue`() {
        val model = orders().apply { rescue("charge") }
        assertTrue(model.step("charge").hasRescueStep)
        assertTrue(model.step("charge_rescue").isRescueStep)
    }

    @Test
    fun `a rescue step shares the script of a step of the same name`() {
        val model = Model().apply { declare("orders", listOf("charge", "charge_rescue")) }
        val step = model.step("charge_rescue")
        model.rescue("charge")
        assertSame(step, model.step("charge_rescue"))
        assertTrue(step.isRescueStep)
    }

    @Test
    fun `initial data holds each key once`() {
        val model = orders().apply { contains("amount", 10L) }
        assertThrows<ModelException> { model.contains("amount", 20L) }
    }

    @Test
    fun `initial data cannot follow an empty data bag`() {
        val model = orders().apply { emptyDataBag() }
        assertThrows<ModelException> { model.contains("amount", 10L) }
    }

    @Test
    fun `an empty data bag cannot follow initial data`() {
        val model = orders().apply { contains("amount", 10L) }
        assertThrows<ModelException> { model.emptyDataBag() }
    }

    @Test
    fun `global metadata keeps every entry in the order set`() {
        val model =
            orders().apply {
                globalMetadata("tenant", "acme")
                globalMetadata("tenant", "globex")
            }
        assertEquals(listOf("tenant" to "acme", "tenant" to "globex"), model.initialGlobalMetadata)
    }

    @Test
    fun `what the executor dispatches with is stated once`() {
        val model = orders().apply { dispatchWith(Dispatching.Default) }
        assertThrows<ModelException> { model.dispatchWith(Dispatching.FactoryFails) }
    }

    @Test
    fun `how a reporter fails is stated once`() {
        val model = orders().apply { fails("audit", ReporterFailure.OnFirstEvent) }
        assertThrows<ModelException> {
            model.fails("audit", ReporterFailure.OnGlobalMetadata("tenant"))
        }
    }

    @Test
    fun `a step's attempts are numbered from 1, in order`() {
        val attempts =
            listOf(ScriptedAttempt(2, Ending.FAILURE), ScriptedAttempt(1, Ending.SUCCESS))
        assertThrows<ModelException> { orders().step("charge").attempts("charge", attempts) }
    }

    @Test
    fun `a step's attempts are stated once`() {
        val step =
            orders().step("charge").apply {
                attempts("charge", listOf(ScriptedAttempt(1, Ending.SUCCESS)))
            }
        assertThrows<ModelException> {
            step.attempts("charge", listOf(ScriptedAttempt(1, Ending.FAILURE)))
        }
    }

    @Test
    fun `a retry budget is never negative`() {
        assertThrows<ModelException> { orders().step("charge").allows("charge", -1) }
    }

    @Test
    fun `a retry budget is stated once`() {
        val step = orders().step("charge").apply { allows("charge", 2) }
        assertThrows<ModelException> { step.allows("charge", 3) }
    }

    @Test
    fun `an outcome the cases do not name is an error in the case`() {
        assertThrows<ModelException> { Ending.named("aborted") }
    }
}
