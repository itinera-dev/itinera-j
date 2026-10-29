package org.itineradev.itinera.workflow.checker

import java.io.File
import java.nio.file.Path
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import kotlin.io.path.Path

/** Where the sources of the checker's tests are, which the tests compile again with the checker. */
internal val sources: Path = Path(System.getProperty("itinera.checker.sources"))

/** The source file of the fixture [type], written in Java. */
internal fun javaSourceOf(type: Class<*>): File =
    sources.resolve("java").resolve(type.name.replace('.', '/') + ".java").toFile()

/** The errors javac reports for [source] when it runs the checker, and only the checker. */
internal fun javacErrors(source: File): List<String> {
    val javac = ToolProvider.getSystemJavaCompiler()
    val diagnostics = DiagnosticCollector<JavaFileObject>()
    javac.getStandardFileManager(null, null, null).use { files ->
        val options = listOf("--release", "17", "-proc:only", "-classpath", classPath())
        val task =
            javac.getTask(null, files, diagnostics, options, null, files.getJavaFileObjects(source))
        task.setProcessors(listOf(JavacChecker()))
        task.call()
    }
    return diagnostics.diagnostics.filter(::isError).map { diagnostic ->
        diagnostic.getMessage(null)
    }
}

private fun isError(diagnostic: Diagnostic<*>): Boolean = diagnostic.kind == Diagnostic.Kind.ERROR

private fun classPath(): String =
    listOfNotNull(System.getProperty("java.class.path"), System.getProperty("jdk.module.path"))
        .joinToString(File.pathSeparator)
