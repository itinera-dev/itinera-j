package org.itineradev.itinera.workflow.checker

import java.net.URI
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
        val source =
            """
            import org.itineradev.itinera.workflow.ItineraWorkflow;
            import org.itineradev.itinera.workflow.WithStep;

            @WithStep(step = Charge.class, name = "charge")
            public final class Shop implements ItineraWorkflow {}
            """
        assertEquals(
            listOf("Charge.run returns String, not Outcome"),
            compiled(source, Generating("Charge", CHARGE), sources = generated).map(::firstLine),
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
 * What javac reports compiling [source], the class `Shop`, with the checker and [others], which
 * generate their sources into [sources].
 */
private fun compiled(
    source: String,
    vararg others: Processor,
    sources: Path? = null,
): List<Diagnostic<*>> {
    val file =
        object :
            SimpleJavaFileObject(URI.create("string:///Shop.java"), JavaFileObject.Kind.SOURCE) {
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence =
                source.trimIndent()
        }
    val reported = DiagnosticCollector<JavaFileObject>()
    val options =
        listOf("-proc:only", "-classpath", System.getProperty("java.class.path")) +
            sources?.let(::generatingInto).orEmpty()
    val compiling =
        ToolProvider.getSystemJavaCompiler()
            .getTask(null, null, reported, options, null, listOf(file))
    compiling.setProcessors(listOf(JavacChecker()) + others)
    compiling.call()
    return reported.diagnostics
}

private fun generatingInto(directory: Path): List<String> = listOf("-s", directory.toString())

private fun located(diagnostic: Diagnostic<*>): String =
    "${diagnostic.lineNumber}: ${diagnostic.getMessage(null)}"

private fun firstLine(diagnostic: Diagnostic<*>): String =
    diagnostic.getMessage(null).lineSequence().first()
