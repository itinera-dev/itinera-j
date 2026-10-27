/**
 * What makes the Kotlin and the Java conformance manifests disagree: Kotlin and Java claim the same
 * cases and proposals, Kotlin's capabilities are Java's with `async`, and Java excludes what Kotlin
 * excludes, by the same scenarios, but for the two tags only Kotlin's types rule out.
 */
object ManifestAgreement {
    /** The capability only Kotlin claims. */
    const val ONLY_KOTLIN_CAPABILITY = "async"

    /** The tags Kotlin may list as impossible and Java never does, since Java can return `null`. */
    val ONLY_KOTLIN_IMPOSSIBLE = setOf("unexpected-result", "non-string-journey-id")

    /** Every disagreement between the two manifests, each as a sentence; none when they agree. */
    fun problems(kotlin: Map<*, *>, java: Map<*, *>): List<String> =
        cases(kotlin, java) +
            proposals(kotlin, java) +
            capabilities(kotlin, java) +
            impossible(kotlin, java)

    private fun cases(kotlin: Map<*, *>, java: Map<*, *>) =
        listOfNotNull(
            "The manifests pin different cases: ${kotlin["cases"]} for Kotlin, ${java["cases"]} for Java."
                .takeIf { kotlin["cases"] != java["cases"] }
        )

    private fun proposals(kotlin: Map<*, *>, java: Map<*, *>): List<String> {
        val kotlinOnly = numbers(kotlin) - numbers(java)
        val javaOnly = numbers(java) - numbers(kotlin)
        return listOfNotNull(
            "Only Kotlin lists the proposals ${kotlinOnly.sorted()}."
                .takeIf { kotlinOnly.isNotEmpty() },
            "Only Java lists the proposals ${javaOnly.sorted()}.".takeIf { javaOnly.isNotEmpty() },
        )
    }

    private fun capabilities(kotlin: Map<*, *>, java: Map<*, *>): List<String> {
        val expected = strings(java, "capabilities") + ONLY_KOTLIN_CAPABILITY
        return listOfNotNull(
            "Java claims `$ONLY_KOTLIN_CAPABILITY`, which only Kotlin claims."
                .takeIf { ONLY_KOTLIN_CAPABILITY in strings(java, "capabilities") },
            "Kotlin claims ${strings(kotlin, "capabilities").sorted()}, but Java's capabilities with `$ONLY_KOTLIN_CAPABILITY` are ${expected.sorted()}."
                .takeIf { strings(kotlin, "capabilities") != expected },
        )
    }

    private fun impossible(kotlin: Map<*, *>, java: Map<*, *>): List<String> {
        val kotlinTags = exclusions(kotlin)
        val javaTags = exclusions(java)
        val expectedJava = kotlinTags.keys - ONLY_KOTLIN_IMPOSSIBLE
        val listedOnlyByKotlin = expectedJava - javaTags.keys
        val listedOnlyByJava = javaTags.keys - expectedJava - ONLY_KOTLIN_IMPOSSIBLE
        val listedByJavaNever = javaTags.keys.intersect(ONLY_KOTLIN_IMPOSSIBLE)
        val differing =
            expectedJava.intersect(javaTags.keys).filter { kotlinTags.differsFrom(javaTags, it) }
        return listOfNotNull(
            "Only Kotlin excludes ${listedOnlyByKotlin.sorted()}."
                .takeIf { listedOnlyByKotlin.isNotEmpty() },
            "Only Java excludes ${listedOnlyByJava.sorted()}."
                .takeIf { listedOnlyByJava.isNotEmpty() },
            "Java excludes ${listedByJavaNever.sorted()}, which Java can write."
                .takeIf { listedByJavaNever.isNotEmpty() },
        ) + differing.sorted().map(::differentScenarios)
    }

    private fun Map<String, *>.differsFrom(other: Map<String, *>, tag: String) =
        this[tag] != other[tag]

    private fun differentScenarios(tag: String) =
        "The manifests exclude different scenarios under `$tag`."

    private fun numbers(manifest: Map<*, *>): Set<Int> =
        (manifest["proposals"] as? List<*>)
            .orEmpty()
            .filterIsInstance<Number>()
            .map(Number::toInt)
            .toSet()

    private fun strings(manifest: Map<*, *>, key: String): Set<String> =
        (manifest[key] as? List<*>).orEmpty().filterIsInstance<String>().toSet()

    private fun exclusions(manifest: Map<*, *>): Map<String, Set<Pair<Any?, Any?>>> =
        (manifest["impossible"] as? Map<*, *>).orEmpty().entries.associate(::tagAndScenarios)

    private fun tagAndScenarios(entry: Map.Entry<*, *>) =
        entry.key.toString() to scenariosOf(entry.value)

    private fun scenariosOf(scenarios: Any?): Set<Pair<Any?, Any?>> =
        (scenarios as? List<*>)
            .orEmpty()
            .filterIsInstance<Map<*, *>>()
            .map(::featureAndScenario)
            .toSet()

    private fun featureAndScenario(exclusion: Map<*, *>) =
        exclusion["feature"] to exclusion["scenario"]
}
