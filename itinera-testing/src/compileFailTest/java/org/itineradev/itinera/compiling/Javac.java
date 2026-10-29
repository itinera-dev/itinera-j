package org.itineradev.itinera.compiling;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/** Compiles Java snippets against the suite's class path, into a directory of their own. */
public final class Javac {
  private static final JavaCompiler JAVAC = ToolProvider.getSystemJavaCompiler();

  private final Path directory;

  public Javac(Path directory) {
    this.directory = directory;
  }

  /** What javac made of a snippet. */
  public record Compiled(boolean compiled, String messages) {}

  public Compiled compile(String snippet) throws IOException {
    var source = Files.writeString(directory.resolve("Snippet.java"), snippet);
    var messages = new StringWriter();
    try (var files = JAVAC.getStandardFileManager(null, null, null)) {
      var options =
          List.of("--release", "17", "-classpath", classPath(), "-d", directory.toString());
      var compiled =
          JAVAC
              .getTask(messages, files, null, options, null, files.getJavaFileObjects(source))
              .call();
      return new Compiled(compiled, messages.toString());
    }
  }

  private static String classPath() {
    return String.join(
        File.pathSeparator,
        Stream.of("java.class.path", "jdk.module.path")
            .map(System::getProperty)
            .filter(Objects::nonNull)
            .toList());
  }
}
