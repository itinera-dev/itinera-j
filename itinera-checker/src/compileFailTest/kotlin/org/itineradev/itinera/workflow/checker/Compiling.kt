@file:OptIn(ExperimentalCompilerApi::class)

package org.itineradev.itinera.workflow.checker

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import kotlin.io.path.Path
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Assertions.assertNotEquals

/** Where the sources of the checker's tests are, which the tests compile again with the checker. */
internal val sources: Path = Path(System.getProperty("itinera.checker.sources"))

/** The source file of the fixture [type], written in Java. */
internal fun javaSourceOf(type: Class<*>): File =
    sources.resolve("java").resolve(type.name.replace('.', '/') + ".java").toFile()

/** The source file of the fixture [type], written in Kotlin. */
internal fun kotlinSourceOf(type: Class<*>): File =
    sources.resolve("kotlin").resolve(type.name.replace('.', '/') + ".kt").toFile()

/**
 * The errors javac reports for [source] when it runs the checker, which it starts on its own, and
 * no annotation processor.
 */
internal fun javacErrors(source: File): List<String> {
    val javac = ToolProvider.getSystemJavaCompiler()
    val diagnostics = DiagnosticCollector<JavaFileObject>()
    val classes = Files.createTempDirectory("classes").toFile()
    javac.getStandardFileManager(null, null, null).use { files ->
        val options = listOf("--release", "17", "-d", classes.path, "-classpath", classPath())
        val task =
            javac.getTask(null, files, diagnostics, options, null, files.getJavaFileObjects(source))
        task.setProcessors(emptyList())
        task.call()
    }
    classes.deleteRecursively()
    return diagnostics.diagnostics.filter(::isError).map { diagnostic ->
        diagnostic.getMessage(null)
    }
}

private fun isError(diagnostic: Diagnostic<*>): Boolean = diagnostic.kind == Diagnostic.Kind.ERROR

private fun classPath(): String =
    listOfNotNull(System.getProperty("java.class.path"), System.getProperty("jdk.module.path"))
        .joinToString(File.pathSeparator)

/**
 * The errors KSP reports for [source], written in Kotlin or in Java, when it runs the checker, and
 * only the checker.
 */
internal fun kspErrors(source: File): List<String> {
    val compilation =
        KotlinCompilation().apply {
            sources = listOf(sourceFileOf(source))
            inheritClassPath = true
            jvmTarget = "17"
            messageOutputStream = OutputStream.nullOutputStream()
            configureKsp { symbolProcessorProviders += KspChecker() }
        }
    val result = compilation.compile()
    assertNotEquals(KotlinCompilation.ExitCode.INTERNAL_ERROR, result.exitCode, result.messages)
    return result.messages.lines().mapNotNull(::kspError)
}

private fun sourceFileOf(source: File): SourceFile =
    if (source.extension == "java") SourceFile.java(source.name, source.readText())
    else SourceFile.kotlin(source.name, source.readText())

/** The message of a line KSP prints for an error, without where in the source it was found. */
private fun kspError(line: String): String? = kspErrorLine.matchEntire(line)?.groupValues?.last()

private val kspErrorLine = Regex("""e: \[ksp] (?:\S+:\d+: )?(.*)""")
