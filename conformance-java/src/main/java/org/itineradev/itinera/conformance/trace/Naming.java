package org.itineradev.itinera.conformance.trace;

import java.util.List;
import java.util.stream.Stream;
import org.itineradev.itinera.event.BuildSource;
import org.itineradev.itinera.event.ByAdapter;
import org.itineradev.itinera.event.ByAdapterForKey;
import org.itineradev.itinera.event.ByPolicy;
import org.itineradev.itinera.event.ByProvider;
import org.itineradev.itinera.event.ByProviderForKey;
import org.itineradev.itinera.event.ByStepHook;
import org.itineradev.itinera.event.ByWorkflowHook;
import org.itineradev.itinera.event.FailureRequester;
import org.itineradev.itinera.event.JourneyAbort;
import org.itineradev.itinera.event.MissingData;
import org.itineradev.itinera.event.NotAValue;
import org.itineradev.itinera.event.PolicyCouldNotBeBuilt;
import org.itineradev.itinera.event.Requester;
import org.itineradev.itinera.event.RequiredDataMissing;
import org.itineradev.itinera.event.ReservedMetadataKey;
import org.itineradev.itinera.event.ResultSource;
import org.itineradev.itinera.event.StepCouldNotBeBuilt;
import org.itineradev.itinera.event.UnexpectedResult;
import org.itineradev.itinera.event.ValueSource;
import org.itineradev.itinera.event.WrongData;
import org.itineradev.itinera.event.WrongType;

/** The cells naming who an abort blames. */
final class Naming {
  /** The key a case writes for an abort on a request for the failure's reason. */
  private static final String FAILURE_REASON = "failure reason";

  /** The key a case writes for an abort on a request for the error. */
  private static final String ERROR = "error";

  private Naming() {}

  /**
   * The cells naming what {@code abort} names besides its step and its code: the key, and the
   * adapter, provider, policy or hook it blames.
   */
  static List<Cell> cellsOf(JourneyAbort abort) {
    if (abort instanceof StepCouldNotBeBuilt built) {
      return builtCells(built.source());
    }
    if (abort instanceof PolicyCouldNotBeBuilt built) {
      return List.of(new Cell("policy", built.policy()));
    }
    if (abort instanceof RequiredDataMissing missing) {
      return cellsOf(missing.missing());
    }
    if (abort instanceof WrongType wrong) {
      return cellsOf(wrong.wrong());
    }
    if (abort instanceof NotAValue.UnderKey notAValue) {
      return keyed(notAValue.key(), valueCells(notAValue.source()));
    }
    if (abort instanceof NotAValue notAValue) {
      return valueCells(notAValue.getSource());
    }
    if (abort instanceof UnexpectedResult unexpected) {
      return resultCells(unexpected.source());
    }
    if (abort instanceof ReservedMetadataKey reserved) {
      return keyed(reserved.key(), valueCells(reserved.source()));
    }
    return List.of();
  }

  /** The cells naming a hook of a policy. */
  static List<Cell> hookCells(Object policy, Object hook) {
    return List.of(new Cell("policy", policy), new Cell("hook", hook));
  }

  private static List<Cell> cellsOf(MissingData missing) {
    if (missing instanceof MissingData.Key key) {
      return keyed(key.key(), requesterCells(key.requester()));
    }
    if (missing instanceof MissingData.FailureReason reason) {
      return keyed(FAILURE_REASON, failureCells(reason.requester()));
    }
    return keyed(ERROR, failureCells(((MissingData.Error) missing).requester()));
  }

  private static List<Cell> cellsOf(WrongData wrong) {
    if (wrong instanceof WrongData.Key key) {
      return keyed(key.key(), requesterCells(key.requester()));
    }
    return keyed(FAILURE_REASON, failureCells(((WrongData.FailureReason) wrong).requester()));
  }

  private static List<Cell> requesterCells(Requester requester) {
    if (requester instanceof ByAdapter adapter) {
      return List.of(new Cell("adapter", adapter.adapter()));
    }
    if (requester instanceof ByProvider provider) {
      return List.of(new Cell("provider", provider.provider()));
    }
    if (requester instanceof ByStepHook hook) {
      return hookCells(hook.policy(), hook.hook());
    }
    if (requester instanceof ByWorkflowHook hook) {
      return hookCells(hook.policy(), hook.hook());
    }
    return List.of();
  }

  private static List<Cell> failureCells(FailureRequester requester) {
    return requester instanceof ByStepHook hook ? hookCells(hook.policy(), hook.hook()) : List.of();
  }

  private static List<Cell> builtCells(BuildSource source) {
    if (source instanceof ByAdapterForKey adapter) {
      return List.of(new Cell("adapter", adapter.adapter()), new Cell("key", adapter.key()));
    }
    if (source instanceof ByProvider provider) {
      return List.of(new Cell("provider", provider.provider()));
    }
    return List.of();
  }

  private static List<Cell> valueCells(ValueSource source) {
    if (source instanceof ByProvider provider) {
      return List.of(new Cell("provider", provider.provider()));
    }
    if (source instanceof ByStepHook hook) {
      return hookCells(hook.policy(), hook.hook());
    }
    if (source instanceof ByWorkflowHook hook) {
      return hookCells(hook.policy(), hook.hook());
    }
    return List.of();
  }

  private static List<Cell> resultCells(ResultSource source) {
    if (source instanceof ByAdapterForKey adapter) {
      return List.of(new Cell("adapter", adapter.adapter()), new Cell("key", adapter.key()));
    }
    if (source instanceof ByProviderForKey provider) {
      return List.of(new Cell("provider", provider.provider()), new Cell("key", provider.key()));
    }
    if (source instanceof ByStepHook hook) {
      return hookCells(hook.policy(), hook.hook());
    }
    if (source instanceof ByPolicy policy) {
      return List.of(new Cell("policy", policy.policy()));
    }
    return List.of();
  }

  private static List<Cell> keyed(String key, List<Cell> cells) {
    return Stream.concat(Stream.of(new Cell("key", key)), cells.stream()).toList();
  }
}
