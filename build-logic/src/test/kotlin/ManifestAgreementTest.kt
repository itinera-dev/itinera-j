import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

class ManifestAgreementTest {
    @ParameterizedTest
    @MethodSource("agreements")
    fun `manifests that agree pass`(manifests: Pair<Map<String, Any>, Map<String, Any>>) {
        assertEquals(
            emptyList<String>(),
            ManifestAgreement.problems(manifests.first, manifests.second),
        )
    }

    @ParameterizedTest
    @MethodSource("disagreements")
    fun `manifests that disagree are refused`(manifests: Pair<Map<String, Any>, Map<String, Any>>) {
        assertTrue(ManifestAgreement.problems(manifests.first, manifests.second).isNotEmpty())
    }

    companion object {
        @JvmStatic
        fun agreements() =
            listOf(
                    "manifests listing nothing" to (kotlin() to java()),
                    "Kotlin excluding what only its types rule out" to
                        (kotlin(
                            impossible =
                                mapOf(
                                    "unexpected-result" to listOf(scenario("a")),
                                    "non-string-journey-id" to listOf(scenario("b")),
                                )
                        ) to java()),
                    "each language naming its own proof" to
                        (kotlin(
                            impossible =
                                mapOf("bag-write" to listOf(scenario("a", proof = "Kotlin's test")))
                        ) to
                            java(
                                impossible =
                                    mapOf(
                                        "bag-write" to listOf(scenario("a", proof = "Java's test"))
                                    )
                            )),
                    "proposals listed in another order" to
                        (kotlin(proposals = listOf(2, 121)) to java(proposals = listOf(121, 2))),
                )
                .map { (name, manifests) -> arguments(named(name, manifests)) }

        @JvmStatic
        fun disagreements() =
            listOf(
                    "other cases" to (kotlin(cases = "v0.1.0-rc.17") to java()),
                    "a proposal only Kotlin lists" to (kotlin(proposals = listOf(121)) to java()),
                    "a proposal only Java lists" to (kotlin() to java(proposals = listOf(121))),
                    "Java claiming async" to
                        (kotlin() to java(capabilities = listOf("sync", "async"))),
                    "Kotlin not claiming async" to
                        (kotlin(capabilities = listOf("sync")) to java()),
                    "a capability only Kotlin claims besides async" to
                        (kotlin(capabilities = listOf("sync", "async", "testing")) to java()),
                    "a capability only Java claims" to
                        (kotlin() to java(capabilities = listOf("sync", "testing"))),
                    "an exclusion only Kotlin lists" to
                        (kotlin(impossible = mapOf("bag-write" to listOf(scenario("a")))) to
                            java()),
                    "an exclusion only Java lists" to
                        (kotlin() to
                            java(impossible = mapOf("bag-write" to listOf(scenario("a"))))),
                    "Java excluding unexpected results" to
                        (kotlin(impossible = mapOf("unexpected-result" to listOf(scenario("a")))) to
                            java(impossible = mapOf("unexpected-result" to listOf(scenario("a"))))),
                    "one tag excluding other scenarios" to
                        (kotlin(impossible = mapOf("bag-write" to listOf(scenario("a")))) to
                            java(impossible = mapOf("bag-write" to listOf(scenario("b"))))),
                )
                .map { (name, manifests) -> arguments(named(name, manifests)) }

        private fun kotlin(
            cases: String = "v0.1.0-rc.18",
            proposals: List<Int> = emptyList(),
            capabilities: List<String> = listOf("sync", "async"),
            impossible: Map<String, List<Map<String, Any>>> = emptyMap(),
        ) = manifest(cases, proposals, capabilities, impossible)

        private fun java(
            proposals: List<Int> = emptyList(),
            capabilities: List<String> = listOf("sync"),
            impossible: Map<String, List<Map<String, Any>>> = emptyMap(),
        ) = manifest("v0.1.0-rc.18", proposals, capabilities, impossible)

        private fun manifest(
            cases: String,
            proposals: List<Int>,
            capabilities: List<String>,
            impossible: Map<String, List<Map<String, Any>>>,
        ): Map<String, Any> =
            mapOf(
                "cases" to cases,
                "proposals" to proposals,
                "capabilities" to capabilities,
                "impossible" to impossible,
            )

        private fun scenario(name: String, proof: String = "a test") =
            mapOf(
                "feature" to "cases/tier-1/f.feature",
                "scenario" to name,
                "proof" to mapOf("test" to proof),
            )
    }
}
