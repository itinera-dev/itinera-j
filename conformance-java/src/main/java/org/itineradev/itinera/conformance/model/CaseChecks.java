package org.itineradev.itinera.conformance.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The checks of a case that need the whole model, such as one comparing a building block with the
 * workflow it is bound to, made once every Given sentence is written.
 */
public final class CaseChecks {
  private final List<Consumer<Model>> checks = new ArrayList<>();

  CaseChecks() {}

  /** Adds a check, made after those added before. */
  public void add(Consumer<Model> check) {
    checks.add(check);
  }

  /** Makes every check on {@code model}, the whole model of the case. */
  public void make(Model model) {
    for (var check : checks) {
      check.accept(model);
    }
  }
}
