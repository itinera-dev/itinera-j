package org.itineradev.itinera.journey.samples

import org.itineradev.itinera.journey.JourneyResult
import org.itineradev.itinera.journey.OutputException
import org.itineradev.itinera.journey.OutputProblem
import org.itineradev.itinera.value.TypeToken

fun outputs(result: JourneyResult): String {
    return try {
        val receipt = result.output(TypeToken.of(Receipt::class.java))
        "order ${receipt.order} charged ${receipt.total}"
    } catch (unread: OutputException) {
        when (val problem = unread.problem) {
            OutputProblem.Aborted -> "aborted"
            is OutputProblem.Absent -> "nothing under ${problem.key}"
            is OutputProblem.OtherType -> "another type under ${problem.key}"
        }
    }
}
