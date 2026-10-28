package org.itineradev.itinera.conformance.sentences;

import java.util.List;
import java.util.stream.Stream;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.run.Entry;
import org.itineradev.itinera.conformance.run.ScenarioReporter;
import org.itineradev.itinera.event.Event;

/** What the scripted reporters of a scenario received. */
final class Reported {
  private Reported() {}

  /**
   * The scripted reporters made under {@code name}.
   *
   * @throws ModelException when the scenario made none, so no sentence can read what it received
   */
  static List<ScenarioReporter> madeAs(World world, String name) {
    var made = world.recorders().made(name);
    if (made.isEmpty()) {
      throw new ModelException("no reporter \"" + name + "\" was made");
    }
    return made;
  }

  /** Every event the scripted reporters made under {@code name} received. */
  static List<Event> eventsOf(World world, String name) {
    return madeAs(world, name).stream().flatMap(Reported::eventsOf).toList();
  }

  /** Every global metadata entry the scripted reporters made under {@code name} received. */
  static List<Entry> entriesOf(World world, String name) {
    return madeAs(world, name).stream().flatMap(Reported::entriesOf).toList();
  }

  private static Stream<Event> eventsOf(ScenarioReporter reporter) {
    return reporter.events().stream();
  }

  private static Stream<Entry> entriesOf(ScenarioReporter reporter) {
    return reporter.entries().stream();
  }
}
