/**
 * Itinera's compile-time checks: the mistakes of annotated workflows reported as compile errors,
 * through javac in Java.
 */
module org.itineradev.itinera.checker {
  requires java.compiler;
  requires kotlin.stdlib;

  exports org.itineradev.itinera.workflow.checker;

  provides javax.annotation.processing.Processor with
      org.itineradev.itinera.workflow.checker.JavacChecker;
}
