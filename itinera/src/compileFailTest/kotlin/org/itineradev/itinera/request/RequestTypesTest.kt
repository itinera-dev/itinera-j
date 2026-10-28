package org.itineradev.itinera.request

import org.itineradev.itinera.compiling.assertDoesNotCompile
import org.junit.jupiter.api.Test

private const val REQUESTS =
    """
    import org.itineradev.itinera.request.*
    import org.itineradev.itinera.value.TypeToken
    import org.itineradev.itinera.workflow.*

    val AMOUNT = Requests.input("amount", TypeToken.of(Long::class.javaObjectType))
    val DISCOUNT = Requests.optionalInput("discount", TypeToken.of(Long::class.javaObjectType))
    """

class RequestTypesTest {
    @Test
    fun `a request's answer is of the type its token declares`() {
        assertDoesNotCompile(
            "$REQUESTS fun read(got: Got<StepKind<Sync>>): String = got[AMOUNT]",
            "Return type mismatch: expected 'String', actual 'Long'.",
        )
    }

    @Test
    fun `an optional request's answer may be absent`() {
        assertDoesNotCompile(
            "$REQUESTS fun read(got: Got<StepKind<Sync>>): Long = got[DISCOUNT]",
            "Return type mismatch: expected 'Long', actual 'Long?'.",
        )
    }
}
