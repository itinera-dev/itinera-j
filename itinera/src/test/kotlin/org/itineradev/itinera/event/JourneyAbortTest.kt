package org.itineradev.itinera.event

import org.itineradev.itinera.event.Fixtures.audit
import org.itineradev.itinera.event.Fixtures.charge
import org.itineradev.itinera.event.Fixtures.closing
import org.itineradev.itinera.event.Fixtures.payments
import org.itineradev.itinera.event.Fixtures.pricing
import org.itineradev.itinera.event.samples.aborts
import org.itineradev.itinera.event.samples.missing
import org.itineradev.itinera.event.samples.wrong
import org.itineradev.itinera.journey.AbortReason
import org.itineradev.itinera.policy.StepHook
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

class JourneyAbortTest {
    @ParameterizedTest
    @FieldSource("aborts")
    fun `every abort names its reason, and the error only when failing code caused it`(
        abort: JourneyAbort,
        reason: AbortReason,
        errorMessage: String?,
    ) {
        assertEquals(reason, abort.reason)
        assertEquals(errorMessage, abort.errorMessage)
    }

    @Test
    fun `an abort for the reserved key names journey_id`() {
        assertEquals("journey_id", ReservedMetadataKey(charge, ByStep).key)
    }

    @Test
    fun `the samples tell why a journey was aborted and what was missing or of the wrong type`() {
        assertEquals(
            "hook failed during charge: no ledger",
            aborts(HookFailed(charge, "no ledger")),
        )
        assertEquals("the failure's reason", missing(MissingData.FailureReason(ByRescueStep)))
        assertEquals("the key amount", wrong(WrongData.Key("amount", closing)))
    }

    companion object {
        private val retryHook = ByStepHook(audit, StepHook.ON_STEP_RETRY)

        @JvmField
        val aborts =
            listOf(
                argumentSet(
                    "a failing adapter",
                    StepCouldNotBeBuilt(charge, ByAdapterForKey(pricing, "price"), "offline"),
                    AbortReason.STEP_COULD_NOT_BE_BUILT,
                    "offline",
                ),
                argumentSet(
                    "a provider that made no instance",
                    StepCouldNotBeBuilt(charge, ByProvider(payments), null),
                    AbortReason.STEP_COULD_NOT_BE_BUILT,
                    null,
                ),
                argumentSet(
                    "a failing policy",
                    PolicyCouldNotBeBuilt(charge, audit, "no ledger"),
                    AbortReason.POLICY_COULD_NOT_BE_BUILT,
                    "no ledger",
                ),
                argumentSet(
                    "a missing key",
                    RequiredDataMissing(charge, MissingData.Key("price", ByAdapter(pricing))),
                    AbortReason.REQUIRED_DATA_MISSING,
                    null,
                ),
                argumentSet(
                    "a wrong type",
                    WrongType(null, WrongData.Key("amount", closing), null),
                    AbortReason.WRONG_TYPE,
                    null,
                ),
                argumentSet(
                    "a wrong type whose copy threw",
                    WrongType(charge, WrongData.Key("ticket", ByStep), "no more tickets"),
                    AbortReason.WRONG_TYPE,
                    "no more tickets",
                ),
                argumentSet(
                    "an invalid lifecycle",
                    InvalidLifecycle(charge),
                    AbortReason.INVALID_LIFECYCLE,
                    null,
                ),
                argumentSet(
                    "a contribution that is not a value",
                    NotAValue.UnderKey(charge, ByStep, "paid", null),
                    AbortReason.NOT_A_VALUE,
                    null,
                ),
                argumentSet(
                    "a contribution whose copy threw",
                    NotAValue.UnderKey(charge, ByStep, "ticket", "no more tickets"),
                    AbortReason.NOT_A_VALUE,
                    "no more tickets",
                ),
                argumentSet(
                    "event data that is not a value",
                    NotAValue.InEvent(charge, retryHook, EventKind.JOURNEY_INFO, null),
                    AbortReason.NOT_A_VALUE,
                    null,
                ),
                argumentSet(
                    "details that are not a value",
                    NotAValue.InDetails(charge, ByStep, null),
                    AbortReason.NOT_A_VALUE,
                    null,
                ),
                argumentSet(
                    "a failing hook",
                    HookFailed(charge, "no ledger"),
                    AbortReason.HOOK_FAILED,
                    "no ledger",
                ),
                argumentSet(
                    "a failing reporter",
                    ReporterFailed(null, "disk full"),
                    AbortReason.REPORTER_FAILED,
                    "disk full",
                ),
                argumentSet(
                    "a skipping rescue step",
                    RescueSkipped(charge),
                    AbortReason.RESCUE_SKIPPED,
                    null,
                ),
                argumentSet(
                    "the reserved key",
                    ReservedMetadataKey(charge, retryHook),
                    AbortReason.RESERVED_METADATA_KEY,
                    null,
                ),
                argumentSet(
                    "an unexpected result",
                    UnexpectedResult(charge, ByPolicy(audit), "the constructor returned null"),
                    AbortReason.UNEXPECTED_RESULT,
                    null,
                ),
            )
    }
}
