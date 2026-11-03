package org.itineradev.itinera.workflow.checker.samples

import java.net.URI
import java.nio.file.Files
import javax.tools.Diagnostic
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider

fun checkingJava(): List<String> {
    val shop =
        """
        import org.itineradev.itinera.step.ItineraStep;
        import org.itineradev.itinera.step.Step;
        import org.itineradev.itinera.workflow.ItineraWorkflow;
        import org.itineradev.itinera.workflow.WithStep;
        import org.itineradev.itinera.workflow.Workflow;

        @Workflow(name = "shop")
        @WithStep(step = Shop.Charge.class, name = "charge")
        public final class Shop implements ItineraWorkflow {
          @Step
          public static final class Charge implements ItineraStep {
            public String run() {
              return "charged";
            }
          }
        }
        """
            .trimIndent()
    val source =
        object :
            SimpleJavaFileObject(URI.create("string:///Shop.java"), JavaFileObject.Kind.SOURCE) {
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence = shop
        }
    val reported = DiagnosticCollector<JavaFileObject>()
    val classes = Files.createTempDirectory("classes").toFile()
    val classPath = System.getProperty("java.class.path")
    // itinera-checker is on the processor path, where javac finds the checker and starts it.
    val options = listOf("-d", classes.path, "-classpath", classPath, "-processorpath", classPath)
    val javac = ToolProvider.getSystemJavaCompiler()
    javac.getTask(null, null, reported, options, null, listOf(source)).call()
    classes.deleteRecursively()
    return reported.diagnostics.map(::message)
}

private fun message(diagnostic: Diagnostic<*>): String = diagnostic.getMessage(null)
