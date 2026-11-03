package org.itineradev.itinera.testing

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * One mistake in a test, with the step, the attempt or the service provider it concerns, as the
 * test named them. A [TestMistake] holds every one found.
 *
 * Kotlin matches a mistake with `when`, and Java with `switch` or `instanceof`.
 *
 * @sample org.itineradev.itinera.testing.samples.mistakes
 */
@ExperimentalItineraApi
public sealed interface Mistake {
    /** What kind of mistake it is. */
    public val kind: MistakeKind

    /** What went wrong, in a sentence. */
    public val description: String

    /**
     * The script names a step, or the test reads the record of a step, that the workflow does not
     * have.
     *
     * @sample org.itineradev.itinera.testing.samples.mistakes
     */
    @JvmRecord
    public data class UnknownStep(
        /** The name given. */
        val step: String
    ) : Mistake {
        override val kind: MistakeKind
            get() = MistakeKind.UNKNOWN_STEP

        override val description: String
            get() = "the workflow has no step $step"
    }

    /**
     * The script names a step it named before.
     *
     * @sample org.itineradev.itinera.testing.samples.mistakes
     */
    @JvmRecord
    public data class StepScriptedTwice(
        /** The step named twice. */
        val step: String
    ) : Mistake {
        override val kind: MistakeKind
            get() = MistakeKind.STEP_SCRIPTED_TWICE

        override val description: String
            get() = "$step is scripted twice"
    }

    /**
     * The script describes an attempt of a step before describing every attempt before it.
     *
     * @sample org.itineradev.itinera.testing.samples.mistakes
     */
    @JvmRecord
    public data class GapInTheAttempts(
        /** The step. */
        val step: String,
        /** The attempt described too early. */
        val attempt: Int,
    ) : Mistake {
        override val kind: MistakeKind
            get() = MistakeKind.GAP_IN_THE_ATTEMPTS

        override val description: String
            get() = "attempt $attempt of $step is described before the attempt before it"
    }

    /**
     * The script chooses attempts of a step it has already described.
     *
     * @sample org.itineradev.itinera.testing.samples.mistakes
     */
    @JvmRecord
    public data class AttemptScriptedTwice(
        /** The step. */
        val step: String,
        /** The first attempt the choice describes again. */
        val attempt: Int,
    ) : Mistake {
        override val kind: MistakeKind
            get() = MistakeKind.ATTEMPT_SCRIPTED_TWICE

        override val description: String
            get() = "attempt $attempt of $step is described twice"
    }

    /**
     * The script chooses an attempt the step can never have: one below 1, or one past its retry
     * budget plus one, and one more for a step with a rescue step.
     *
     * @sample org.itineradev.itinera.testing.samples.mistakes
     */
    @JvmRecord
    public data class NoSuchAttempt(
        /** The step. */
        val step: String,
        /** The attempt it can never have. */
        val attempt: Int,
    ) : Mistake {
        override val kind: MistakeKind
            get() = MistakeKind.NO_SUCH_ATTEMPT

        override val description: String
            get() = "$step can have no attempt $attempt"
    }

    /**
     * The script makes a rescue step skip, which a rescue step never does.
     *
     * @sample org.itineradev.itinera.testing.samples.mistakes
     */
    @JvmRecord
    public data class RescueStepSkips(
        /** The rescue step. */
        val step: String
    ) : Mistake {
        override val kind: MistakeKind
            get() = MistakeKind.RESCUE_STEP_SKIPS

        override val description: String
            get() = "$step is a rescue step, which never skips"
    }

    /**
     * The script contributes for a step that declares no contributor, which the real step could not
     * do.
     *
     * @sample org.itineradev.itinera.testing.samples.mistakes
     */
    @JvmRecord
    public data class NoContributor(
        /** The step. */
        val step: String
    ) : Mistake {
        override val kind: MistakeKind
            get() = MistakeKind.NO_CONTRIBUTOR

        override val description: String
            get() = "$step declares no contributor, so it cannot contribute"
    }

    /**
     * The script emits, or sets global metadata, for a step that declares no step reporter, which
     * the real step could not do.
     *
     * @sample org.itineradev.itinera.testing.samples.mistakes
     */
    @JvmRecord
    public data class NoReporter(
        /** The step. */
        val step: String
    ) : Mistake {
        override val kind: MistakeKind
            get() = MistakeKind.NO_REPORTER

        override val description: String
            get() = "$step declares no step reporter, so it cannot emit or set global metadata"
    }

    /**
     * A scripted step does not describe an attempt it can have, given how its earlier attempts end.
     *
     * @sample org.itineradev.itinera.testing.samples.mistakes
     */
    @JvmRecord
    public data class AttemptLeftOut(
        /** The step. */
        val step: String,
        /** The first attempt left out, since the attempts after it depend on how it ends. */
        val attempt: Int,
    ) : Mistake {
        override val kind: MistakeKind
            get() = MistakeKind.ATTEMPT_LEFT_OUT

        override val description: String
            get() = "attempt $attempt of $step is left out"
    }

    /**
     * A scripted step can fail on attempt 1, and its rescue step is not scripted.
     *
     * @sample org.itineradev.itinera.testing.samples.mistakes
     */
    @JvmRecord
    public data class RescueStepLeftOut(
        /** The rescue step left out. */
        val step: String,
        /** The step it rescues, which can fail on attempt 1. */
        val rescued: String,
    ) : Mistake {
        override val kind: MistakeKind
            get() = MistakeKind.RESCUE_STEP_LEFT_OUT

        override val description: String
            get() = "$step is left out, though $rescued can fail on attempt 1"
    }

    /**
     * The test asked that every step be scripted, and this one is not.
     *
     * @sample org.itineradev.itinera.testing.samples.mistakes
     */
    @JvmRecord
    public data class StepLeftOut(
        /** The step left out. */
        val step: String
    ) : Mistake {
        override val kind: MistakeKind
            get() = MistakeKind.STEP_LEFT_OUT

        override val description: String
            get() = "$step is left out, though every step must be scripted"
    }

    /**
     * A fake is named after a service provider the workflow does not have.
     *
     * @sample org.itineradev.itinera.testing.samples.mistakes
     */
    @JvmRecord
    public data class UnknownProvider(
        /** The name given. */
        val provider: String
    ) : Mistake {
        override val kind: MistakeKind
            get() = MistakeKind.UNKNOWN_PROVIDER

        override val description: String
            get() = "the workflow has no service provider $provider"
    }

    /**
     * A fake makes another service than its service provider makes.
     *
     * @sample org.itineradev.itinera.testing.samples.mistakes
     */
    @JvmRecord
    public data class AnotherService(
        /** The service provider the fake is named after. */
        val provider: String
    ) : Mistake {
        override val kind: MistakeKind
            get() = MistakeKind.ANOTHER_SERVICE

        override val description: String
            get() = "the fake of $provider makes another service than the provider"
    }
}
