package org.itineradev.itinera.conformance.run

import org.itineradev.itinera.conformance.model.Emitted
import org.itineradev.itinera.conformance.model.ModelException
import org.itineradev.itinera.conformance.model.ScriptRow
import org.itineradev.itinera.conformance.model.ScriptedEnding
import org.itineradev.itinera.executor.testing.AttemptChoice
import org.itineradev.itinera.executor.testing.AttemptContributing
import org.itineradev.itinera.executor.testing.AttemptEmitting
import org.itineradev.itinera.executor.testing.AttemptScript
import org.itineradev.itinera.executor.testing.AttemptSettingGlobalMetadata
import org.itineradev.itinera.executor.testing.NamedStep
import org.itineradev.itinera.report.testing.Level

/**
 * Writes a script's rows through test support, in the order written, for a script whose steps are
 * of type [S]: [first] names the first step, and [next] each step after it, named on the script so
 * far.
 */
internal class ScriptWriting<S : AttemptChoice<S>>(
    private val first: (String) -> NamedStep<S>,
    private val next: (S, String) -> NamedStep<S>,
) {
    /** The script [rows] write, which names at least one step. */
    fun written(rows: List<ScriptRow>): S =
        rows.fold(Writing<S>(null, null), ::writtenWith).script
            ?: throw ModelException("the script names no step")

    private fun writtenWith(writing: Writing<S>, row: ScriptRow): Writing<S> {
        val script = writing.script
        val chosen =
            if (script != null && row.step == writing.step) chosen(script, row.attempt)
            else namedFor(script, row)
        return Writing(played(chosen, row), row.step)
    }

    /**
     * The attempts the first row of a step chooses, on the step it names: the step itself when the
     * row leaves attempt 1 unsaid.
     */
    private fun namedFor(script: S?, row: ScriptRow): AttemptScript<S> {
        val named = script?.let { written -> next(written, row.step) } ?: first(row.step)
        return row.attempt?.let { attempt -> chosen(named, attempt) } ?: named
    }
}

/** The script written so far, and the step its last row named. */
private class Writing<S>(val script: S?, val step: String?)

private fun <S> chosen(choice: AttemptChoice<S>, attempt: String?): AttemptScript<S> =
    when (attempt) {
        null -> throw ModelException("only the first row of a step may leave its attempt unsaid")
        "every" -> choice.onEveryAttempt()
        "remaining" -> choice.onRemainingAttempts()
        else ->
            choice.onAttempt(
                attempt.toIntOrNull()
                    ?: throw ModelException("$attempt is not an attempt the JVM can write")
            )
    }

/** What the attempts [chosen] does, as [row] says, and the script it leaves. */
private fun <S> played(chosen: AttemptScript<S>, row: ScriptRow): S =
    if (row.ending == ScriptedEnding.REAL_RUN) runForReal(chosen, row) else acted(chosen, row)

private fun <S> runForReal(chosen: AttemptScript<S>, row: ScriptRow): S {
    if (fillsAnotherColumn(row)) {
        throw ModelException("the real run of ${row.step} fills another column")
    }
    return chosen.willRunForReal()
}

private fun fillsAnotherColumn(row: ScriptRow): Boolean =
    row.contributes.isNotEmpty() ||
        row.setsGlobalMetadata.isNotEmpty() ||
        listOfNotNull(row.emits, row.emitMessage, row.code, row.message).isNotEmpty()

private fun <S> acted(chosen: AttemptScript<S>, row: ScriptRow): S {
    val contributing =
        row.contributes.entries.fold<Map.Entry<String, Any?>, AttemptContributing<S>>(chosen) {
            script,
            entry ->
            script.willContribute(entry.key, entry.value)
        }
    val emitting: AttemptEmitting<S> =
        row.emits?.let { emitted -> contributing.willEmit(levelOf(emitted), messageOf(row)) }
            ?: contributing
    val setting =
        row.setsGlobalMetadata.entries.fold<
            Map.Entry<String, Any?>,
            AttemptSettingGlobalMetadata<S>,
        >(
            emitting
        ) { script, entry ->
            script.willSetGlobalMetadata(entry.key, entry.value)
        }
    return when (row.ending) {
        ScriptedEnding.SUCCESS -> setting.willSucceed()
        ScriptedEnding.FAILURE -> setting.willFail(codeOf(row))
        ScriptedEnding.SKIPPED -> row.code?.let(setting::willSkipBecause) ?: setting.willSkip()
        ScriptedEnding.ABNORMAL_TERMINATION ->
            setting.willTerminateAbnormally(row.message ?: UNWRITTEN_MESSAGE)
        ScriptedEnding.REAL_RUN -> throw ModelException("a real run plays nothing")
    }
}

private fun levelOf(emitted: Emitted): Level =
    when (emitted) {
        Emitted.STEP_INFO -> Level.INFO
        Emitted.STEP_WARNING -> Level.WARNING
        Emitted.STEP_ERROR -> Level.ERROR
    }

private fun messageOf(row: ScriptRow): String =
    row.emitMessage ?: throw ModelException("the row of ${row.step} emits with no message")

private fun codeOf(row: ScriptRow): String =
    row.code ?: throw ModelException("the row of ${row.step} fails with no code")
