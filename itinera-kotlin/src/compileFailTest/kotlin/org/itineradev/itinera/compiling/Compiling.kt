@file:OptIn(ExperimentalCompilerApi::class)

package org.itineradev.itinera.compiling

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import java.io.OutputStream
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/** Compiles [source], a Kotlin file using itinera, against the tests' class path. */
private fun compile(source: String): JvmCompilationResult =
    KotlinCompilation()
        .apply {
            sources = listOf(SourceFile.kotlin("Snippet.kt", source))
            inheritClassPath = true
            jvmTarget = "17"
            messageOutputStream = OutputStream.nullOutputStream()
        }
        .compile()

/** Asserts that [source] does not compile, with an error that says [expected]. */
internal fun assertDoesNotCompile(source: String, expected: String) {
    val result = compile(source)
    assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode, result.messages)
    assertTrue(expected in result.messages, result.messages)
}
