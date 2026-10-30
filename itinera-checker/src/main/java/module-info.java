/**
 * Itinera's compile-time checks: the mistakes of annotated workflows reported as compile errors,
 * through KSP in Kotlin and javac in Java.
 */
// KSP's API is an automatic module, named after its file, and only the compiler running KSP reads
// it.
@SuppressWarnings("requires-automatic")
module org.itineradev.itinera.checker {
  requires java.compiler;
  requires jdk.compiler;
  requires kotlin.stdlib;
  requires static symbol.processing.api;

  exports org.itineradev.itinera.workflow.checker;

  provides com.sun.source.util.Plugin with
      org.itineradev.itinera.workflow.checker.JavacChecker;
  provides com.google.devtools.ksp.processing.SymbolProcessorProvider with
      org.itineradev.itinera.workflow.checker.KspChecker;
}
