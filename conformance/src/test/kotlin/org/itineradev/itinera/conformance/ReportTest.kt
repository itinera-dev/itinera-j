package org.itineradev.itinera.conformance

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tools.jackson.databind.json.JsonMapper

internal class ReportTest {
    @TempDir lateinit var directory: Path

    private val json = JsonMapper.builder().build()

    private fun report(name: String, written: String): Path =
        Files.writeString(directory.resolve(name), written)

    private fun merges(local: String, async: String): String =
        merged(
                mapOf(
                    Column.SYNCHRONOUS to report("local.json", local),
                    Column.ASYNCHRONOUS to report("async.json", async),
                )
            )
            .toString()

    private fun reads(written: String): String = json.readTree(written).toString()

    @Test
    fun `each feature lists every column's scenarios, each tagged with its column`() {
        assertEquals(
            reads(
                """[
                {"uri": "a.feature", "elements": [
                    {"name": "one", "line": 3, "tags": [{"name": "@x"}, {"name": "@executor-local"}]},
                    {"name": "one", "line": 4, "tags": [{"name": "@x"}, {"name": "@executor-async"}]}]},
                {"uri": "b.feature", "elements": [
                    {"name": "two", "line": 5, "tags": [{"name": "@executor-local"}]}]},
                {"uri": "c.feature", "elements": [
                    {"name": "three", "line": 7, "tags": [{"name": "@executor-async"}]}]}]"""
            ),
            merges(
                """[{"uri": "a.feature", "elements": [{"name": "one", "line": 3, "tags": [{"name": "@x"}]}]},
                    {"uri": "b.feature", "elements": [{"name": "two", "line": 5}]}]""",
                """[{"uri": "a.feature", "elements": [{"name": "one", "line": 3, "tags": [{"name": "@x"}]}]},
                    {"uri": "c.feature", "elements": [{"name": "three", "line": 7}]}]""",
            ),
        )
    }

    @Test
    fun `a second copy moves past every scenario sharing its name`() {
        val rows =
            """[{"uri": "a.feature", "elements": [{"name": "row", "line": 10}, {"name": "row", "line": 11}]}]"""
        assertEquals(
            reads(
                """[{"uri": "a.feature", "elements": [
                    {"name": "row", "line": 10, "tags": [{"name": "@executor-local"}]},
                    {"name": "row", "line": 11, "tags": [{"name": "@executor-local"}]},
                    {"name": "row", "line": 12, "tags": [{"name": "@executor-async"}]},
                    {"name": "row", "line": 13, "tags": [{"name": "@executor-async"}]}]}]"""
            ),
            merges(rows, rows),
        )
    }

    @Test
    fun `a scenario that ran in one column keeps its line`() {
        assertEquals(
            reads(
                """[{"uri": "a.feature", "elements": [
                    {"name": "sync", "line": 3, "tags": [{"name": "@executor-local"}]},
                    {"name": "async", "line": 6, "tags": [{"name": "@executor-async"}]}]}]"""
            ),
            merges(
                """[{"uri": "a.feature", "elements": [{"name": "sync", "line": 3}]}]""",
                """[{"uri": "a.feature", "elements": [{"name": "async", "line": 6}]}]""",
            ),
        )
    }

    @Test
    fun `reports of no scenario merge into an empty report`() {
        assertEquals("[]", merges("[]", "[]"))
    }
}
