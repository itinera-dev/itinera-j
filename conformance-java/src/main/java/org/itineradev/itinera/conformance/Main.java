package org.itineradev.itinera.conformance;

/** Runs the selected scenarios and writes the report, failing when any failed. */
public final class Main {
  /** The package of the sentences. */
  private static final String SENTENCES = "org.itineradev.itinera.conformance.sentences";

  private Main() {}

  /** Runs the scenarios the process's environment selects. */
  public static void main(String[] arguments) {
    Environment environment;
    try {
      environment = Environment.fromProcess();
    } catch (EnvironmentException error) {
      System.err.println(error.getMessage());
      System.exit(2);
      return;
    }
    if (!run(environment)) {
      System.exit(1);
    }
  }

  /**
   * Runs the scenarios {@code environment} selects and writes their report, telling whether every
   * scenario passed. A scenario that uses a sentence the runner does not define fails.
   */
  static boolean run(Environment environment) {
    String[] arguments = {
      "--glue",
      SENTENCES,
      "--tags",
      environment.tags(),
      "--plugin",
      "json:" + environment.report().toAbsolutePath(),
      "--plugin",
      "summary",
      "--monochrome",
      environment.cases().toString(),
    };
    return io.cucumber.core.cli.Main.run(arguments, Thread.currentThread().getContextClassLoader())
        == 0;
  }
}
