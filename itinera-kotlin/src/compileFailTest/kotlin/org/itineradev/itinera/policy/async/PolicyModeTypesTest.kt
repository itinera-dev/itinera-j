package org.itineradev.itinera.policy.async

import org.itineradev.itinera.compiling.MODES
import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

private const val POLICIES =
    """
    $MODES
    import org.itineradev.itinera.policy.*
    import org.itineradev.itinera.policy.async.*

    class Audit

    val audit = StepPolicyDescriptor.builder("audit", PolicyFactory(::Audit))
    val asyncAudit = asyncStepPolicy("audit", PolicyFactory(::Audit))
    """

class PolicyModeTypesTest {
    @ParameterizedTest
    @FieldSource("hooks")
    fun `a policy takes only hooks of its own mode`(policy: String, refused: String) {
        assertDoesNotCompile(
            "$POLICIES val hooked = $policy.onStepSuccess($refused { _, _ -> OnSuccess.keepDefault() })",
            "Argument type mismatch",
        )
    }

    @ParameterizedTest
    @FieldSource("reporters")
    fun `a hook reads only the reporter of its own mode`(mode: String, refused: String) {
        assertDoesNotCompile(
            "$POLICIES fun read(got: Got<StepHookKind<$mode>>) = got[Requests.$refused()]",
            "Argument type mismatch",
        )
    }

    companion object {
        @JvmField
        val hooks =
            listOf(
                argumentSet("a synchronous policy", "audit", "AsyncHook"),
                argumentSet("an asynchronous policy", "asyncAudit", "SyncHook"),
            )

        @JvmField
        val reporters =
            listOf(
                argumentSet("a synchronous hook", "Sync", "asyncHookReporter"),
                argumentSet("an asynchronous hook", "Async", "hookReporter"),
            )
    }
}
