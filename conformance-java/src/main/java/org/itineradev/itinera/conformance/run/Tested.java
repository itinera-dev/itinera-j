package org.itineradev.itinera.conformance.run;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.itineradev.itinera.conformance.model.FakeModel;
import org.itineradev.itinera.conformance.model.Model;
import org.itineradev.itinera.conformance.model.TestSupport;
import org.itineradev.itinera.instance.WorkflowInstance;
import org.itineradev.itinera.instance.testing.TestInstanceBuilder;
import org.itineradev.itinera.report.testing.RecordingReporter;
import org.itineradev.itinera.workflow.Sync;
import org.itineradev.itinera.workflow.WorkflowDescriptor;

/**
 * The test support a scenario's journeys use, as the scenario declares: for the last instance
 * created, what its recording reporter kept and its fakes.
 */
public final class Tested {
  private final TestSupport testing;
  private Supplier<Kept> keeping;
  private Map<String, CountedFake> fakes = Map.of();

  /** The test support that {@code testing} declares. */
  public Tested(TestSupport testing) {
    this.testing = testing;
  }

  /** The fakes of the last instance, by the name of the provider each replaces. */
  public Map<String, CountedFake> fakes() {
    return fakes;
  }

  /** What the recording reporter of the last instance kept. */
  public Kept kept() {
    if (keeping == null) {
      throw new Unmet("expected the recording reporter of test support; none was made");
    }
    return keeping.get();
  }

  /**
   * The instance of the workflow {@code descriptor} describes, created through test support for
   * {@code scenario} as {@code model} declares, after the reporters the workflow lists, with the
   * recording reporter of test support and the fakes.
   */
  WorkflowInstance<Sync> instance(
      WorkflowDescriptor<Scenario, Sync> descriptor, Scenario scenario, Model model) {
    var builder = TestInstanceBuilder.of(descriptor, scenario).initialData(model.initialData());
    for (var entry : model.initialGlobalMetadata()) {
      builder.reportersGlobalMetadata(entry.getKey(), entry.getValue());
    }
    if (testing.recordingReporter()) {
      var recording = new RecordingReporter();
      builder.reporter(recording);
      keeping =
          () -> new Kept(recording.getEvents(), recording.getGlobalMetadata(), recording.getPath());
    }
    fakes =
        testing.fakes().entrySet().stream()
            .collect(
                Collectors.toMap(
                    Map.Entry::getKey,
                    Tested::counted,
                    (earlier, later) -> later,
                    LinkedHashMap::new));
    fakes.forEach((provider, fake) -> faking(builder, provider, fake.service(), fake));
    return builder.create();
  }

  private static CountedFake counted(Map.Entry<String, FakeModel> fake) {
    return new CountedFake(fake.getValue());
  }

  private static <T> void faking(
      TestInstanceBuilder builder, String provider, Class<T> service, CountedFake fake) {
    builder.fakeProvider(provider, service, () -> fake.provided(service));
  }
}
