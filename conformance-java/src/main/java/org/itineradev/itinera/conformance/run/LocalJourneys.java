package org.itineradev.itinera.conformance.run;

import static org.itineradev.itinera.conformance.record.Dispatchers.dispatchers;
import static org.itineradev.itinera.conformance.record.Dispatchers.failingDispatchers;

import java.util.List;
import java.util.stream.IntStream;
import org.itineradev.itinera.conformance.model.DispatcherScript;
import org.itineradev.itinera.conformance.model.Dispatching;
import org.itineradev.itinera.conformance.model.IdGenerator;
import org.itineradev.itinera.conformance.model.Model;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.executor.LocalExecutor;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.instance.InstanceBuilder;
import org.itineradev.itinera.instance.WorkflowInstance;
import org.itineradev.itinera.workflow.StepDescriptor;
import org.itineradev.itinera.workflow.Sync;
import org.itineradev.itinera.workflow.WorkflowDescriptor;

/**
 * The journeys of the workflow the scenario declares, put together with the builder, each step a
 * step mock the witness watches, run on {@code LocalExecutor}.
 */
public final class LocalJourneys implements Journeys {
  private final WorkflowDescriptor<Scenario, Sync> descriptor;
  private final Model model;
  private final Recorders recorders;

  private LocalJourneys(
      WorkflowDescriptor<Scenario, Sync> descriptor, Model model, Recorders recorders) {
    this.descriptor = descriptor;
    this.model = model;
    this.recorders = recorders;
  }

  /**
   * Puts the workflow the scenario declares together.
   *
   * @throws org.itineradev.itinera.workflow.Violations when the builder refuses the workflow
   * @throws NotRunYet when the scenario scripts what the runner cannot run yet
   */
  public static Journeys of(Model model, Recorders recorders, Witness witness) {
    var workflow = model.workflow();
    var builder = WorkflowDescriptor.builder(Scenario.class, workflow.name());
    for (var step : workflow.steps()) {
      builder.step(stepOf(step, model, witness));
    }
    if (workflow.idGenerator() instanceof IdGenerator.Returns generator) {
      builder.idGenerator((scenario, data) -> generator.id());
    }
    return new LocalJourneys(builder.build(), model, recorders);
  }

  private static StepDescriptor<Sync> stepOf(String name, Model model, Witness witness) {
    var script = model.step(name);
    if (script.isAsynchronous()) {
      throw new ModelException(
          "Java hands no asynchronous step to LocalExecutor: it writes synchronous workflows only");
    }
    var builder = StepDescriptor.builder(name, witness.watching(name, Scripts.mockOf(script)));
    return Scripts.scriptedBy(builder, script).build();
  }

  @Override
  public List<Journey> run(int times) {
    var executor = executorOf(model.dispatching());
    return IntStream.range(0, times).mapToObj(each -> journeyOn(executor)).toList();
  }

  private Journey journeyOn(LocalExecutor executor) {
    WorkflowInstance<Sync> instance;
    try {
      instance = instance();
    } catch (IllegalStateException failure) {
      return new Journey(null, new Ended.NotCreated(failure));
    }
    return new Journey(instance.getJourneyId().toString(), endedOf(executor, instance));
  }

  private static Ended endedOf(LocalExecutor executor, WorkflowInstance<Sync> instance) {
    try {
      return new Ended.Returned(executor.run(instance));
    } catch (Refusal refusal) {
      return new Ended.Refused(refusal);
    }
  }

  private WorkflowInstance<Sync> instance() {
    var builder = InstanceBuilder.of(descriptor, new Scenario()).initialData(model.initialData());
    for (var entry : model.initialGlobalMetadata()) {
      builder.reportersGlobalMetadata(entry.getKey(), entry.getValue());
    }
    var reporters = model.workflow().reporters();
    for (var name : reporters == null ? List.<String>of() : reporters) {
      builder.reporter(recorders.make(name));
    }
    return builder.create();
  }

  private LocalExecutor executorOf(Dispatching dispatching) {
    if (dispatching instanceof Dispatching.Default) {
      return new LocalExecutor();
    }
    if (dispatching instanceof Dispatching.FactoryFails) {
      return new LocalExecutor(failingDispatchers());
    }
    if (dispatching instanceof Dispatching.Holding holding) {
      return new LocalExecutor(dispatchers(recorders.make(holding.reporter()), holding.script()));
    }
    return new LocalExecutor(
        dispatchers(recorders.make(Recorders.RUNNERS_RECORDER), new DispatcherScript.Delivers()));
  }
}
