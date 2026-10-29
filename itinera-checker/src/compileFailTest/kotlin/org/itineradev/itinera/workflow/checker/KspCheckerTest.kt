@file:OptIn(ExperimentalCompilerApi::class)

package org.itineradev.itinera.workflow.checker

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import java.io.OutputStream
import org.itineradev.itinera.workflow.checker.samples.checkingKotlin
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KspCheckerTest {
    @Test
    fun `KSP reports a mistake in an annotated workflow as a compile error`() {
        assertEquals(listOf("Shop\$Charge.run returns String, not Outcome"), checkingKotlin())
    }

    @Test
    fun `KSP reports each mistake once, at the declaration that makes it`() {
        val source =
            """
            import org.itineradev.itinera.step.ItineraStep
            import org.itineradev.itinera.step.Step
            import org.itineradev.itinera.workflow.ItineraWorkflow
            import org.itineradev.itinera.workflow.WithStep

            class Shop {
                @Step
                class Charge : ItineraStep {
                    fun run(): String = "charged"
                }

                @WithStep(Charge::class, name = "charge")
                class Orders : ItineraWorkflow

                @WithStep(Charge::class, name = "charge")
                @WithStep(Charge::class, name = "charge")
                class Refunds : ItineraWorkflow
            }
            """
        assertEquals(
            listOf(
                "9: Shop\$Charge.run returns String, not Outcome",
                "17: duplicate step name: two steps are named charge",
            ),
            compiled(SourceFile.kotlin("Shop.kt", source.trimIndent())).map(::located),
        )
    }

    @Test
    fun `KSP checks a Java workflow only when it names a part written in Kotlin`() {
        val charge =
            """
            import org.itineradev.itinera.step.ItineraStep
            import org.itineradev.itinera.step.Step

            @Step
            class KotlinCharge : ItineraStep {
                fun run(): String = "charged"
            }
            """
        val shop =
            """
            import org.itineradev.itinera.step.ItineraStep;
            import org.itineradev.itinera.step.Step;
            import org.itineradev.itinera.workflow.ItineraWorkflow;
            import org.itineradev.itinera.workflow.WithStep;

            public final class Shop {
              @Step
              public static final class JavaCharge implements ItineraStep {
                public String run() {
                  return "charged";
                }
              }

              @WithStep(step = KotlinCharge.class, name = "charge")
              public static final class Orders implements ItineraWorkflow {}

              @WithStep(step = JavaCharge.class, name = "charge")
              public static final class Refunds implements ItineraWorkflow {}
            }
            """
        assertEquals(
            listOf("KotlinCharge.run returns String, not Outcome"),
            compiled(
                    SourceFile.kotlin("KotlinCharge.kt", charge.trimIndent()),
                    SourceFile.java("Shop.java", shop.trimIndent()),
                )
                .map(KspError::message),
        )
    }

    @Test
    fun `KSP reads a workflow once a later round generates the class it names`() {
        val source =
            """
            import org.itineradev.itinera.workflow.ItineraWorkflow
            import org.itineradev.itinera.workflow.WithStep

            @WithStep(Charge::class, name = "charge")
            class Shop : ItineraWorkflow
            """
        assertEquals(
            listOf("Charge.run returns String, not Outcome"),
            compiled(SourceFile.kotlin("Shop.kt", source.trimIndent()), generating = CHARGE)
                .map(KspError::message),
        )
    }

    @Test
    fun `KSP reads a workflow once a later round generates what its parts request`() {
        val source =
            """
            import org.itineradev.itinera.step.Input
            import org.itineradev.itinera.step.ItineraStep
            import org.itineradev.itinera.step.Outcome
            import org.itineradev.itinera.step.Step
            import org.itineradev.itinera.workflow.ItineraWorkflow
            import org.itineradev.itinera.workflow.WithStep

            @Step
            class Charge(@Input("lines") private val lines: List<Line>) : ItineraStep {
                fun run(): Outcome = if (lines.isEmpty()) fail("empty") else succeed()
            }

            @WithStep(Charge::class, name = "charge")
            class Shop : ItineraWorkflow
            """
        assertEquals(
            emptyList<KspError>(),
            compiled(SourceFile.kotlin("Shop.kt", source.trimIndent()), generating = LINE),
        )
    }

    @Test
    fun `KSP reads a workflow once a later round generates what its superclass requests`() {
        val source =
            """
            import org.itineradev.itinera.policy.FromWorkflow
            import org.itineradev.itinera.step.Input
            import org.itineradev.itinera.step.ItineraStep
            import org.itineradev.itinera.step.Outcome
            import org.itineradev.itinera.step.Step
            import org.itineradev.itinera.workflow.InputAdapter
            import org.itineradev.itinera.workflow.ItineraWorkflow
            import org.itineradev.itinera.workflow.Supplied
            import org.itineradev.itinera.workflow.WithStep

            @Step
            class Charge(@Input("amount") private val amount: Long) : ItineraStep {
                fun run(): Outcome = if (amount > 0) succeed() else fail("free")
            }

            abstract class Pricing : ItineraWorkflow {
                @InputAdapter(steps = ["charge"])
                fun priced(@FromWorkflow("line") line: Line): Supplied = supply(line.product.length)
            }

            @WithStep(Charge::class, name = "charge")
            class Shop : Pricing()
            """
        assertEquals(
            emptyList<KspError>(),
            compiled(SourceFile.kotlin("Shop.kt", source.trimIndent()), generating = LINE),
        )
    }
}

/** An error KSP reported, at [line] of its source, if it named one. */
private data class KspError(val line: String, val message: String)

/**
 * The errors KSP reports compiling [sources] with the checker, and with a processor that generates
 * [generating], when given, in its first round.
 */
private fun compiled(vararg sources: SourceFile, generating: String? = null): List<KspError> {
    val compilation =
        KotlinCompilation().apply {
            this.sources = sources.toList()
            inheritClassPath = true
            messageOutputStream = OutputStream.nullOutputStream()
            configureKsp {
                symbolProcessorProviders += KspChecker()
                symbolProcessorProviders += listOfNotNull(generating?.let(::Generating))
            }
        }
    return compilation.compile().messages.lines().mapNotNull(::kspError)
}

private fun kspError(line: String): KspError? =
    kspErrorLine.matchEntire(line)?.destructured?.let(::errorOf)

private fun errorOf(parts: MatchResult.Destructured): KspError =
    KspError(parts.component1(), parts.component2())

private val kspErrorLine = Regex("""e: \[ksp] (?:\S+:(\d+): )?(.*)""")

private fun located(error: KspError): String = "${error.line}: ${error.message}"

/** Generates, in its first round, a Kotlin file holding [source]. */
private class Generating(private val source: String) : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        Generator(environment.codeGenerator, source)
}

private class Generator(private val files: CodeGenerator, private val source: String) :
    SymbolProcessor {
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (!generated)
            files.createNewFile(Dependencies(false), "", "Generated").writer().use { writer ->
                writer.write(source)
            }
        generated = true
        return emptyList()
    }
}

/** A data class, which a step may request. */
private const val LINE = "data class Line(val product: String)"

/** A step class whose run returns what a step's run never does. */
private const val CHARGE =
    """
    @org.itineradev.itinera.step.Step
    class Charge : org.itineradev.itinera.step.ItineraStep {
        fun run(): String = "charged"
    }
    """
