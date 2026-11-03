package org.itineradev.itinera.engine

import org.itineradev.itinera.event.ByStep
import org.itineradev.itinera.event.NotAValue as NotAValueAbort
import org.itineradev.itinera.event.StepCouldNotBeBuilt
import org.itineradev.itinera.event.UnexpectedResult
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.step.Attempt
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Reason
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.Mode
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepDescriptor

/**
 * Builds and runs one attempt of a step of [journey], or of a rescue step, through [parts], from
 * what [building] resolves, with the handles made for the attempt, which close when it ends. An
 * attempt that [scripting] plays is built, and plays in place of its run.
 */
internal class Attempting<M : Mode>(
    private val journey: Journey,
    private val parts: Parts<M>,
    private val building: Building<M>,
    private val scripting: Scripting,
) {
    /**
     * How [attempt] of [step] ended.
     *
     * @throws JourneyAbortSignal once the journey is aborted, even when the step caught the signal
     *   it was given, or a call it left going through aborted it before its handles closed.
     */
    @JvmSynthetic
    suspend fun ended(step: StepDescriptor<M>, attempt: Attempt): Ended {
        val ran =
            attempted(
                Part(step.name, attempt, "step"),
                scripting.step(step.name, attempt),
                { handles -> building.got(step, attempt, handles) },
            ) { got ->
                parts.steps.build(step.factory, got)?.let { running -> running::run }
            }
        return ran.outcome.fold(
            { outcome -> endedWith(step.name, outcome, ran.contributed) },
            Ended::Terminated,
        )
    }

    /**
     * How the one attempt of [rescue], the rescue step of [step], ended, once [step]'s failed
     * attempt left [left].
     *
     * @throws JourneyAbortSignal once the journey is aborted, as [ended] says.
     */
    @JvmSynthetic
    suspend fun rescued(
        step: StepDescriptor<M>,
        rescue: RescueStepDescriptor<M>,
        left: Left,
    ): RescueEnded {
        val ran =
            attempted(
                Part(rescueOf(step.name), FIRST, "rescue step"),
                scripting.rescue(rescueOf(step.name)),
                { handles -> building.rescuing(step, rescue, left, handles) },
            ) { got ->
                parts.steps.rescue(rescue.factory, got)?.let { running -> running::run }
            }
        return ran.outcome.fold(
            { outcome -> rescueEndedWith(rescueOf(step.name), outcome, ran.contributed) },
            RescueEnded::Terminated,
        )
    }

    /**
     * Builds [part] through [build], from what [resolve] gives it with the handles made for its
     * attempt, then runs it, or plays [played] in its place, and gives what its run returned or
     * threw, with what it contributed. Resolving is the engine's work, so only building and running
     * are calls of custom code.
     */
    private suspend fun <G, O : Any> attempted(
        part: Part,
        played: Play<O>?,
        resolve: suspend (Map<Handle, Any>) -> G,
        build: suspend (G) -> (suspend () -> O?)?,
    ): Ran<O> {
        val contributions = Contributions(journey, part.name, ByStep, "The attempt of ${part.name}")
        val emits = StepEmits(journey, part.name, part.attempt)
        val reporter = parts.reporterHandles.stepReporter(emits)
        val handles =
            mapOf(Handle.CONTRIBUTOR to contributions, reporter.handle to reporter.reporter)
        val outcome =
            try {
                val running = built(part, resolve(handles), build)
                val lent = Lent(contributions, emits)
                ran(part, played?.let { play -> playing(play, lent) } ?: running)
            } finally {
                contributions.close()
                reporter.close()
            }
        journey.ensureNotAborted()
        return Ran(outcome, contributions.contributed)
    }

    private suspend fun <G, O : Any> built(
        part: Part,
        got: G,
        build: suspend (G) -> (suspend () -> O?)?,
    ): suspend () -> O? {
        val running =
            parts.calling { build(got) }.getOrElse { thrown -> throw notBuilt(part, thrown) }
        journey.ensureNotAborted()
        return running
            ?: throw unexpected(
                part,
                "the ${part.kind}'s factory returned null, not a ${part.kind}",
            )
    }

    /** What the part's run returned, or what it threw. */
    private suspend fun <O : Any> ran(part: Part, running: suspend () -> O?): Result<O> =
        parts
            .calling { running() }
            .map { outcome ->
                outcome
                    ?: throw unexpected(
                        part,
                        "the ${part.kind}'s run returned null, not an outcome",
                    )
            }

    private fun endedWith(step: StepName, outcome: Outcome, kept: Map<String, Captured>): Ended =
        when (outcome) {
            Outcome.Success -> Ended.Succeeded(kept)
            is Outcome.Failure -> Ended.Failed(checked(step, outcome.reason), kept)
            is Outcome.Skipped -> Ended.Skipped(outcome.reason?.let { checked(step, it) }, kept)
        }

    private fun rescueEndedWith(
        rescue: StepName,
        outcome: RescueOutcome,
        kept: Map<String, Captured>,
    ): RescueEnded =
        when (outcome) {
            RescueOutcome.Success -> RescueEnded.Succeeded(kept)
            is RescueOutcome.Failure -> RescueEnded.Failed(checked(rescue, outcome.reason))
        }

    /**
     * [reason] with a copy of its details that only itinera holds, since the step can still reach
     * the copy its outcome holds.
     *
     * @throws JourneyAbortSignal when they are not a value.
     */
    private fun checked(step: StepName, reason: Reason): Reason =
        when (val capture = Values.capture(reason.details)) {
            is Captured -> reason.copy(details = capture.value)
            is NotAValue -> throw notAValue(step, capture.error)
        }

    private fun notBuilt(part: Part, thrown: Throwable): JourneyAbortSignal =
        journey.abort(Abort(StepCouldNotBeBuilt(part.name, ByStep, messageOf(thrown)), thrown))

    private fun unexpected(part: Part, message: String): JourneyAbortSignal =
        journey.abort(Abort(UnexpectedResult(part.name, ByStep, message), null))

    private fun notAValue(step: StepName, error: Throwable?): JourneyAbortSignal =
        journey.abort(Abort(NotAValueAbort.InDetails(step, ByStep, error?.let(::messageOf)), error))
}

/**
 * The step or rescue step [name], as its [kind] of part is written, being attempted for [attempt].
 */
private class Part(val name: StepName, val attempt: Attempt, val kind: String)

/** What an attempt's run returned or threw, and what it [contributed]. */
private class Ran<O>(val outcome: Result<O>, val contributed: Map<String, Captured>)

/** The first attempt of a step, and a rescue step's only one. */
internal val FIRST: Attempt = Attempt.of(1)

/** What runs in place of a step's run: [play], with what the attempt [lent] it. */
private fun <O> playing(play: Play<O>, lent: Lent): suspend () -> O = { play.play(lent) }
