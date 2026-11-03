package org.itineradev.itinera.conformance.trace

/** Whether [line] is what [expected] describes, comparing only the cells and data it states. */
private fun matches(expected: Line, line: Line): Boolean =
    expected.event == line.event &&
        expected.cells.all { hasCell(line, it) } &&
        admits(expected.data, line.data)

private fun hasCell(line: Line, cell: Map.Entry<String, String>): Boolean =
    line.cells[cell.key] == cell.value

private fun admits(expected: Data, data: Data): Boolean =
    when (expected) {
        Data.Unstated -> true
        Data.None -> data == Data.None
        is Data.Is -> data == expected
    }

/** Whether [lines] hold the [expected] lines in order, possibly with others in between. */
internal fun includesInOrder(lines: List<Line>, expected: List<Line>): Boolean =
    expected.fold<Line, Int?>(0) { from, line -> nextAfter(lines, from, line) } != null

private fun nextAfter(lines: List<Line>, from: Int?, expected: Line): Int? {
    if (from == null) return null
    val found = lines.drop(from).indexOfFirst { matches(expected, it) }
    return if (found == -1) null else from + found + 1
}

/** Whether [lines] are exactly the [expected] lines. */
internal fun areExactly(lines: List<Line>, expected: List<Line>): Boolean =
    lines.size == expected.size && lines.zip(expected).all(::matchesPair)

private fun matchesPair(pair: Pair<Line, Line>): Boolean = matches(pair.second, pair.first)
