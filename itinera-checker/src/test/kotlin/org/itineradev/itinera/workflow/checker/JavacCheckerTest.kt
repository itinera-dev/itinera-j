package org.itineradev.itinera.workflow.checker

import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider
import org.itineradev.itinera.workflow.checker.samples.checkingJava
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class JavacCheckerTest {
    @Test
    fun `javac reports a mistake in an annotated workflow as a compile error`() {
        assertEquals(listOf("Shop\$Charge.run returns String, not Outcome"), checkingJava())
    }

    @Test
    fun `javac reports each mistake once, at the declaration that makes it`() {
        val source =
            """
            import org.itineradev.itinera.step.ItineraStep;
            import org.itineradev.itinera.step.Step;
            import org.itineradev.itinera.workflow.ItineraWorkflow;
            import org.itineradev.itinera.workflow.WithStep;

            public final class Shop {
              @Step
              public static final class Charge implements ItineraStep {
                public String run() {
                  return "charged";
                }
              }

              @WithStep(step = Charge.class, name = "charge")
              public static final class Orders implements ItineraWorkflow {}

              @WithStep(step = Charge.class, name = "charge")
              @WithStep(step = Charge.class, name = "charge")
              public static final class Refunds implements ItineraWorkflow {}
            }
            """
        assertEquals(
            listOf(
                "9: Shop\$Charge.run returns String, not Outcome",
                "19: duplicate step name: two steps are named charge",
            ),
            compiled(source).map(::located),
        )
    }

    @Test
    fun `javac's strict lint finds nothing to warn of in a compilation the checker runs in`() {
        val source =
            """
            import org.itineradev.itinera.step.ItineraStep;
            import org.itineradev.itinera.step.Outcome;
            import org.itineradev.itinera.step.Step;
            import org.itineradev.itinera.workflow.ItineraWorkflow;
            import org.itineradev.itinera.workflow.WithStep;
            import org.itineradev.itinera.workflow.Workflow;

            @Workflow(name = "shop")
            @WithStep(step = Shop.Charge.class, name = "charge")
            public final class Shop implements ItineraWorkflow {
              @Step
              public static final class Charge implements ItineraStep {
                public Outcome run() {
                  return succeed();
                }
              }
            }
            """
        assertEquals(emptyList<String>(), compiled(source, strict = true).map(::firstLine))
    }

    @Test
    fun `javac reports a mistake at its declaration even when its file comes before the workflow's`() {
        assertEquals(
            listOf("Charge.java:3: Charge.run returns String, not Outcome"),
            compiled(NAMING_CHARGE, charge = CHARGE).map(::filed),
        )
    }

    @Test
    fun `javac reports the mistake of a part from a library on the workflow that names it`(
        @TempDir library: Path
    ) {
        compileLibrary(CHARGE, library)
        assertEquals(
            listOf("Shop.java:5: Charge.run returns String, not Outcome"),
            compiled(NAMING_CHARGE, library = library).map(::filed),
        )
    }

    @Test
    fun `javac alone reports a class it cannot find, as the checker leaves its workflow unread`() {
        val source =
            """
            import org.itineradev.itinera.workflow.ItineraWorkflow;
            import org.itineradev.itinera.workflow.WithStep;

            @WithStep(step = Chrage.class, name = "charge")
            public final class Shop implements ItineraWorkflow {}
            """
        assertEquals(listOf("cannot find symbol"), compiled(source).map(::firstLine))
    }

    @Test
    fun `the checker reads a workflow once a later round generates the class it names`(
        @TempDir generated: Path
    ) {
        assertEquals(
            listOf("Charge.run returns String, not Outcome"),
            compiled(NAMING_CHARGE, Generating("Charge", CHARGE), sources = generated)
                .map(::firstLine),
        )
    }

    @Test
    fun `the checker reads the types of a workflow's parts as a later round generates them`(
        @TempDir generated: Path
    ) {
        val source =
            """
            import org.itineradev.itinera.service.Provided;
            import org.itineradev.itinera.service.Provider;
            import org.itineradev.itinera.service.Service;
            import org.itineradev.itinera.step.ItineraStep;
            import org.itineradev.itinera.step.Outcome;
            import org.itineradev.itinera.step.Step;
            import org.itineradev.itinera.workflow.ItineraWorkflow;
            import org.itineradev.itinera.workflow.WithStep;

            @WithStep(step = Shop.Charge.class, name = "charge")
            public final class Shop implements ItineraWorkflow {
              @Step
              public static final class Charge implements ItineraStep {
                public Outcome run(@Service("gateway") Gateway gateway) {
                  return succeed();
                }
              }

              @Provider(key = "gateway")
              public Provided<Gateway> gateway() {
                return noInstance();
              }
            }
            """
        assertEquals(
            emptyList<String>(),
            compiled(source, Generating("Gateway", GATEWAY), sources = generated).map(::firstLine),
        )
    }
}

/** Generates, in its first round, the class [name] from [source]. */
private class Generating(private val name: String, private val source: String) :
    AbstractProcessor() {
    private var generated = false

    override fun getSupportedAnnotationTypes(): Set<String> = setOf("*")

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

    override fun process(annotations: Set<TypeElement>, round: RoundEnvironment): Boolean {
        if (!generated)
            processingEnv.filer.createSourceFile(name).openWriter().use { writer ->
                writer.write(source)
            }
        generated = true
        return false
    }
}

private const val GATEWAY =
    """
    public interface Gateway {
      String take(long amount);
    }
    """

/** A workflow whose only step is `Charge`. */
private const val NAMING_CHARGE =
    """
    import org.itineradev.itinera.workflow.ItineraWorkflow;
    import org.itineradev.itinera.workflow.WithStep;

    @WithStep(step = Charge.class, name = "charge")
    public final class Shop implements ItineraWorkflow {}
    """

/** A step class whose run returns what a step's run never does. */
private const val CHARGE =
    """
    @org.itineradev.itinera.step.Step
    public final class Charge implements org.itineradev.itinera.step.ItineraStep {
      public String run() {
        return "charged";
      }
    }
    """

/**
 * What javac reports compiling [source], the class `Shop`, after [charge], the class `Charge`, when
 * given, with [library] on the class path, with the checker, which javac starts on its own, and
 * [others], which generate their sources into [sources], with every lint and warnings as errors
 * when [strict].
 */
private fun compiled(
    source: String,
    vararg others: Processor,
    sources: Path? = null,
    strict: Boolean = false,
    charge: String? = null,
    library: Path? = null,
): List<Diagnostic<out JavaFileObject>> {
    val files =
        listOfNotNull(charge?.let { text -> sourceFile("Charge", text) }) +
            sourceFile("Shop", source)
    val reported = DiagnosticCollector<JavaFileObject>()
    val classes = Files.createTempDirectory("classes").toFile()
    val classPath =
        listOfNotNull(System.getProperty("java.class.path"), library?.toString())
            .joinToString(File.pathSeparator)
    val options =
        listOf("-d", classes.path, "-classpath", classPath, "-processorpath", classPath) +
            sources?.let(::generatingInto).orEmpty() +
            strictIf(strict)
    val compiling =
        ToolProvider.getSystemJavaCompiler().getTask(null, null, reported, options, null, files)
    if (others.isNotEmpty()) compiling.setProcessors(others.toList())
    compiling.call()
    classes.deleteRecursively()
    return reported.diagnostics
}

/** Compiles [source], the class `Charge`, into [directory], as a library is compiled. */
private fun compileLibrary(source: String, directory: Path) {
    val options =
        listOf("-d", directory.toString(), "-classpath", System.getProperty("java.class.path"))
    ToolProvider.getSystemJavaCompiler()
        .getTask(null, null, null, options, null, listOf(sourceFile("Charge", source)))
        .call()
}

private fun sourceFile(name: String, source: String): JavaFileObject =
    object : SimpleJavaFileObject(URI.create("string:///$name.java"), JavaFileObject.Kind.SOURCE) {
        override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence =
            source.trimIndent()
    }

private fun strictIf(strict: Boolean): List<String> =
    if (strict) listOf("-Xlint:all", "-Werror") else emptyList()

private fun generatingInto(directory: Path): List<String> = listOf("-s", directory.toString())

private fun filed(diagnostic: Diagnostic<out JavaFileObject>): String =
    "${diagnostic.source.toUri().path.substringAfterLast('/')}:${located(diagnostic)}"

private fun located(diagnostic: Diagnostic<*>): String =
    "${diagnostic.lineNumber}: ${diagnostic.getMessage(null)}"

private fun firstLine(diagnostic: Diagnostic<*>): String =
    diagnostic.getMessage(null).lineSequence().first()
