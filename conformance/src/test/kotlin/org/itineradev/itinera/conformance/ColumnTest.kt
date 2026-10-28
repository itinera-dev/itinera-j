package org.itineradev.itinera.conformance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.argumentSet
import org.junit.jupiter.params.provider.FieldSource

internal class ColumnTest {
    @ParameterizedTest
    @FieldSource("columns")
    fun `a column leaves the scenarios of the other mode out of those selected`(
        column: Column,
        tags: String,
    ) {
        assertEquals(tags, column.tags("@proposal-0121 or @proposal-0002"))
    }

    private companion object {
        @JvmStatic
        val columns =
            listOf(
                argumentSet(
                    "the synchronous column",
                    Column.SYNCHRONOUS,
                    "(@proposal-0121 or @proposal-0002) and not @capability-async",
                ),
                argumentSet(
                    "the asynchronous column",
                    Column.ASYNCHRONOUS,
                    "(@proposal-0121 or @proposal-0002) and not @capability-sync",
                ),
            )
    }
}
