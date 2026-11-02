package org.itineradev.itinera.conformance.run;

import static org.itineradev.itinera.conformance.record.Dispatchers.dispatchers;
import static org.itineradev.itinera.conformance.record.Dispatchers.failingDispatchers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.itineradev.itinera.conformance.model.AdapterModel;
import org.itineradev.itinera.conformance.model.DispatcherScript;
import org.itineradev.itinera.conformance.model.Dispatching;
import org.itineradev.itinera.conformance.model.IdGenerator;
import org.itineradev.itinera.conformance.model.Model;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.model.PolicyModel;
import org.itineradev.itinera.conformance.model.ProviderModel;
import org.itineradev.itinera.executor.LocalExecutor;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.instance.InstanceBuilder;
import org.itineradev.itinera.instance.WorkflowInstance;
import org.itineradev.itinera.policy.StepPolicyDescriptor;
import org.itineradev.itinera.service.ProviderDescriptor;
import org.itineradev.itinera.workflow.InputAdapterBuilder;
import org.itineradev.itinera.workflow.InputAdapterDescriptor;
import org.itineradev.itinera.workflow.ListedStep;
import org.itineradev.itinera.workflow.RescueStepDescriptor;
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
  private final Witness witness;

  private LocalJourneys(
      WorkflowDescriptor<Scenario, Sync> descriptor,
      Model model,
      Recorders recorders,
      Witness witness) {
    this.descriptor = descriptor;
    this.model = model;
    this.recorders = recorders;
    this.witness = witness;
  }

  /**
   * Puts the workflow the scenario declares together.
   *
   * @throws org.itineradev.itinera.workflow.Violations when the builder refuses the workflow
   */
  public static Journeys of(Model model, Recorders recorders, Witness witness) {
    var workflow = model.workflow();
    var builder = WorkflowDescriptor.builder(Scenario.class, workflow.name());
    Map<String, StepPolicyDescriptor<?, Sync>> policies =
        workflow.policies().values().stream()
            .filter(Policies::isStepPolicy)
            .collect(
                Collectors.toMap(
                    PolicyModel::name, policy -> Policies.stepPolicyOf(policy, witness)));
    for (var step : workflow.steps()) {
      builder.step(stepOf(step, model, witness, policies));
    }
    for (var adapter : workflow.adapters()) {
      builder.inputAdapter(adapterOf(adapter));
    }
    for (var provider : workflow.providers()) {
      builder.provider(providerOf(provider, witness));
    }
    var reporters = workflow.reporters();
    for (var name : reporters == null ? List.<String>of() : reporters) {
      builder.reporter(journeyId -> recorders.make(name, journeyId));
    }
    var attached = workflow.workflowPolicies();
    for (var name : attached == null ? List.<String>of() : attached) {
      builder.policy(Policies.workflowPolicyOf(workflow.policy(name), witness));
    }
    for (var role : workflow.roles().keySet()) {
      builder.role(Roles.roleOf(role));
    }
    if (workflow.idGenerator() instanceof IdGenerator.Returns generator) {
      builder.idGenerator((scenario, data) -> generator.id());
    }
    return new LocalJourneys(builder.build(), model, recorders, witness);
  }

  private static StepDescriptor<Sync> stepOf(
      String name,
      Model model,
      Witness witness,
      Map<String, StepPolicyDescriptor<?, Sync>> policies) {
    var script = model.step(name);
    if (script.isAsynchronous()) {
      throw new ModelException(
          "Java hands no asynchronous step to LocalExecutor: it writes synchronous workflows only");
    }
    var scripted = new Scripted(script);
    var factory = witness.watching(name, Scripts.mockOf(script), scripted);
    var builder =
        scripted.declaredOn(Scripts.scriptedBy(StepDescriptor.builder(name, factory), script));
    var attached = script.policies();
    for (var policy : attached == null ? List.<String>of() : attached) {
      builder.policy(Policies.attached(policies, policy));
    }
    if (script.hasRescueStep()) {
      builder.rescue(rescueOf(name + "_rescue", model, witness));
    }
    return builder.build();
  }

  private static RescueStepDescriptor<Sync> rescueOf(String name, Model model, Witness witness) {
    var script = model.step(name);
    var scripted = new Scripted(script);
    var factory = witness.watchingRescue(name, Scripts.rescueMockOf(script), scripted);
    return scripted.declaredOn(RescueStepDescriptor.builder(factory)).build();
  }

  private static InputAdapterDescriptor<Scenario, Sync> adapterOf(AdapterModel adapter) {
    if (adapter.isAsynchronous()) {
      throw new ModelException(
          "Java hands no asynchronous input adapter to LocalExecutor: it writes synchronous"
              + " workflows only");
    }
    var adapting = new Adapting(adapter);
    var steps = adapter.steps();
    InputAdapterBuilder<Scenario, Sync> builder =
        InputAdapterDescriptor.builder(
            adapter.name(), steps.get(0), (scenario, got) -> adapting.supplied(got));
    steps.subList(1, steps.size()).forEach(builder::step);
    return adapting.declaredOn(builder).build();
  }

  private static ProviderDescriptor<Scenario, Sync> providerOf(
      ProviderModel provider, Witness witness) {
    if (provider.isAsynchronous()) {
      throw new ModelException(
          "Java hands no asynchronous service provider to LocalExecutor: it writes synchronous"
              + " workflows only");
    }
    return providerOf(provider, witness, Services.classOf(provider.type()));
  }

  private static <T> ProviderDescriptor<Scenario, Sync> providerOf(
      ProviderModel provider, Witness witness, Class<T> type) {
    return switch (provider.scope()) {
      case JOURNEY -> {
        var providing = Providing.ofJourney(provider, witness);
        yield Services.servingOnly(
                providing.declaredOn(
                    ProviderDescriptor.<Scenario, T>builder(
                        provider.name(),
                        provider.key(),
                        type,
                        (scenario, got) -> providing.provided(got, type))),
                provider)
            .build();
      }
      case ATTEMPT -> {
        var providing = Providing.ofAttempt(provider, witness);
        yield Services.servingOnly(
                providing.declaredOn(
                    ProviderDescriptor.<Scenario, T>perAttempt(
                        provider.name(),
                        provider.key(),
                        type,
                        (scenario, got) -> providing.provided(got, type))),
                provider)
            .build();
      }
    };
  }

  @Override
  public List<ListedStep> listing() {
    return descriptor.listing();
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
    var builder =
        InstanceBuilder.of(descriptor, new Scenario(witness, model.workflow().failingOperations()))
            .initialData(model.initialData());
    for (var entry : model.initialGlobalMetadata()) {
      builder.reportersGlobalMetadata(entry.getKey(), entry.getValue());
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
