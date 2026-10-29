@file:OptIn(ExperimentalCompilerApi::class)

package org.itineradev.itinera.workflow.checker.samples

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import java.io.OutputStream
import org.itineradev.itinera.workflow.checker.KspChecker
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

fun checkingKotlin(): List<String> {
    val shop =
        """
        import org.itineradev.itinera.step.ItineraStep
        import org.itineradev.itinera.step.Step
        import org.itineradev.itinera.workflow.ItineraWorkflow
        import org.itineradev.itinera.workflow.WithStep
        import org.itineradev.itinera.workflow.Workflow

        @Workflow(name = "shop")
        @WithStep(Shop.Charge::class, name = "charge")
        class Shop : ItineraWorkflow {
            @Step
            class Charge : ItineraStep {
                fun run(): String = "charged"
            }
        }
        """
            .trimIndent()
    val compilation =
        KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("Shop.kt", shop))
            inheritClassPath = true
            messageOutputStream = OutputStream.nullOutputStream()
            configureKsp { symbolProcessorProviders += KspChecker() }
        }
    return compilation.compile().messages.lines().mapNotNull(::kspError)
}

private fun kspError(line: String): String? = kspErrorLine.matchEntire(line)?.groupValues?.last()

private val kspErrorLine = Regex("""e: \[ksp] \S+:\d+: (.*)""")
