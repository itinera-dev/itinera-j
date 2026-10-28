package org.itineradev.itinera.conformance.run;

import java.util.List;
import org.itineradev.itinera.conformance.model.AdapterAnswer;
import org.itineradev.itinera.conformance.model.AdapterModel;
import org.itineradev.itinera.conformance.model.AdapterRequest;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.journey.DataBagAccess;
import org.itineradev.itinera.request.DataBagAccessKind;
import org.itineradev.itinera.request.Got;
import org.itineradev.itinera.request.InputAdapterKind;
import org.itineradev.itinera.request.KeyKind;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.request.StepNameKind;
import org.itineradev.itinera.request.WorkflowDataKind;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.InputAdapterBuilder;
import org.itineradev.itinera.workflow.Supplied;
import org.itineradev.itinera.workflow.Sync;

/**
 * What a scripted input adapter does when called: it learns the input and the step it is called
 * for, and supplies what its script says for them.
 */
final class Adapting {
  private final AdapterModel adapter;
  private final Request<KeyKind, String> key = Requests.key();
  private final Request<StepNameKind, StepName> step = Requests.stepName();
  private final Request<DataBagAccessKind, DataBagAccess> access;
  private final List<Request<WorkflowDataKind, ?>> fromWorkflow;
  private DataBagAccess kept;

  Adapting(AdapterModel adapter) {
    this.adapter = adapter;
    var requests = adapter.requests();
    access =
        requests.stream().anyMatch(AdapterRequest.DataBag.class::isInstance)
            ? Requests.dataBagAccess()
            : null;
    fromWorkflow =
        requests.stream()
            .filter(AdapterRequest.FromWorkflow.class::isInstance)
            .map(AdapterRequest.FromWorkflow.class::cast)
            .<Request<WorkflowDataKind, ?>>map(Adapting::requestOf)
            .toList();
  }

  /**
   * Declares what the adapter requests on {@code builder}: the input and the step it is called for,
   * its access to the data bag when scripted, and data from the workflow in the order written.
   */
  <W> InputAdapterBuilder<W, Sync> declaredOn(InputAdapterBuilder<W, Sync> builder) {
    builder.request(key).request(step);
    if (access != null) {
      builder.request(access);
    }
    fromWorkflow.forEach(builder::request);
    return builder;
  }

  /** What the adapter supplies for one call, given {@code got}. */
  Supplied supplied(Got<InputAdapterKind> got) {
    var calledFor = got.get(step).toString();
    var given = access == null ? null : got.get(access);
    if (calledFor.equals(adapter.keepsAccessOf())) {
      kept = given;
    }
    var answer = adapter.answerFor(calledFor, got.get(key));
    if (answer instanceof AdapterAnswer.Returns returns) {
      return new Supplied.Value(returns.value());
    }
    if (answer instanceof AdapterAnswer.Nothing) {
      return Supplied.FromDataBag.INSTANCE;
    }
    if (answer instanceof AdapterAnswer.Fails fails) {
      throw new IllegalStateException(fails.message());
    }
    if (answer instanceof AdapterAnswer.NonValue) {
      return new Supplied.Value(NotAValue.INSTANCE);
    }
    if (answer instanceof AdapterAnswer.ItsStepName) {
      return new Supplied.Value(calledFor);
    }
    if (answer instanceof AdapterAnswer.Reads reads) {
      return readThrough(reads.kept() ? kept : given, reads);
    }
    throw new ModelException("the runner has no answer " + answer);
  }

  private static Request<WorkflowDataKind, ?> requestOf(AdapterRequest.FromWorkflow request) {
    return Requests.fromWorkflow(request.key(), TypeToken.of(request.type().type()));
  }

  private static Supplied readThrough(DataBagAccess access, AdapterAnswer.Reads reads) {
    var found =
        access == null
            ? null
            : access.read(reads.key(), TypeToken.of(reads.type().type())).orNull();
    return found == null ? Supplied.FromDataBag.INSTANCE : new Supplied.Value(found);
  }
}
