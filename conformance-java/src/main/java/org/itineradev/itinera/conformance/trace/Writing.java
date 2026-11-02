package org.itineradev.itinera.conformance.trace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.itineradev.itinera.conformance.run.NotRunYet;
import org.itineradev.itinera.event.AttemptStarted;
import org.itineradev.itinera.event.ContributionCommitted;
import org.itineradev.itinera.event.ContributionsDiscarded;
import org.itineradev.itinera.event.DataOverwritten;
import org.itineradev.itinera.event.DecidedBy;
import org.itineradev.itinera.event.DecidingHook;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.event.GiveUpCause;
import org.itineradev.itinera.event.GlobalMetadataSet;
import org.itineradev.itinera.event.InputAdapterFailed;
import org.itineradev.itinera.event.InputAdapterSupplied;
import org.itineradev.itinera.event.JourneyAborted;
import org.itineradev.itinera.event.JourneyFailed;
import org.itineradev.itinera.event.JourneyStarted;
import org.itineradev.itinera.event.JourneySucceeded;
import org.itineradev.itinera.event.OptionalInputAbsent;
import org.itineradev.itinera.event.ServiceProvided;
import org.itineradev.itinera.event.ServiceProviderFailed;
import org.itineradev.itinera.event.StepAbnormalTermination;
import org.itineradev.itinera.event.StepError;
import org.itineradev.itinera.event.StepFailed;
import org.itineradev.itinera.event.StepGivenUp;
import org.itineradev.itinera.event.StepInfo;
import org.itineradev.itinera.event.StepRescued;
import org.itineradev.itinera.event.StepRescuing;
import org.itineradev.itinera.event.StepRetrying;
import org.itineradev.itinera.event.StepSkipped;
import org.itineradev.itinera.event.StepSucceeded;
import org.itineradev.itinera.event.StepWarning;
import org.itineradev.itinera.step.Attempt;
import org.itineradev.itinera.step.Reason;
import org.itineradev.itinera.step.StepName;

/** A line being written, one column after another. */
public final class Writing {
  private final Map<String, String> cells = new LinkedHashMap<>();
  private final List<Object> carried = new ArrayList<>();
  private Data data = new Data.None();

  private Writing() {}

  /**
   * The line of {@code event}, with every column it fills.
   *
   * @throws NotRunYet for an event whose sources the runner cannot read yet
   */
  public static Line lineOf(Event event) {
    var writing = new Writing();
    if (!writing.facts(event) && !writing.decisions(event) && !writing.emitted(event)) {
      throw new NotRunYet("reading " + event.getKind() + " in an event table");
    }
    return new Line(
        event.getKind().toString(),
        Collections.unmodifiableMap(writing.cells),
        writing.data,
        writing.carried);
  }

  private boolean facts(Event event) {
    if (event instanceof JourneyStarted) {
      return true;
    } else if (event instanceof AttemptStarted started) {
      attempt(started.step(), started.attempt());
    } else if (event instanceof StepSucceeded succeeded) {
      attempt(succeeded.step(), succeeded.attempt());
    } else if (event instanceof ContributionsDiscarded discarded) {
      attempt(discarded.step(), discarded.attempt());
    } else if (event instanceof ContributionCommitted committed) {
      EventCells.contributedCells(committed.key(), committed.source()).forEach(this::set);
    } else if (event instanceof DataOverwritten overwritten) {
      EventCells.contributedCells(overwritten.key(), overwritten.source()).forEach(this::set);
    } else if (event instanceof GlobalMetadataSet set) {
      EventCells.metadataCells(set.key(), set.source()).forEach(this::set);
    } else if (event instanceof StepFailed failed) {
      attempted(failed.step(), failed.attempt(), failed.reason());
    } else if (event instanceof StepSkipped skipped) {
      attempted(skipped.step(), skipped.attempt(), skipped.reason());
    } else if (event instanceof StepAbnormalTermination terminated) {
      attempt(terminated.step(), terminated.attempt());
      carried.add(terminated.errorMessage());
    } else if (event instanceof InputAdapterSupplied supplied) {
      adapted(supplied.step(), supplied.attempt(), supplied.key(), supplied.adapter());
    } else if (event instanceof InputAdapterFailed failed) {
      adapted(failed.step(), failed.attempt(), failed.key(), failed.adapter());
    } else if (event instanceof ServiceProviderFailed failed) {
      attempt(failed.step(), failed.attempt());
      set("key", failed.key());
      set("provider", failed.provider());
    } else if (event instanceof JourneyAborted aborted) {
      EventCells.cellsOf(aborted).forEach(this::set);
    } else if (event instanceof OptionalInputAbsent absent) {
      EventCells.cellsOf(absent).forEach(this::set);
    } else if (event instanceof ServiceProvided provided) {
      EventCells.cellsOf(provided).forEach(this::set);
      data = provided.data() == null ? new Data.None() : new Data.Is(provided.data());
    } else {
      return false;
    }
    return true;
  }

  private boolean decisions(Event event) {
    if (event instanceof StepRetrying retrying) {
      decided(retrying.step(), retrying.attempt(), retrying.cause(), retrying.decidedBy());
    } else if (event instanceof StepRescuing rescuing) {
      decided(rescuing.step(), rescuing.attempt(), rescuing.cause(), rescuing.decidedBy());
      set("rescue", rescuing.rescue());
    } else if (event instanceof StepRescued rescued) {
      decided(rescued.step(), rescued.attempt(), null, rescued.decidedBy());
      set("rescue", rescued.rescue());
    } else if (event instanceof StepGivenUp givenUp) {
      decided(givenUp.step(), givenUp.attempt(), givenUp.cause().getCause(), givenUp.decidedBy());
      if (givenUp.cause() instanceof GiveUpCause.FailWorkflow failing) {
        reason(failing.reason());
      }
    } else if (event instanceof JourneySucceeded succeeded) {
      decidedBy(succeeded.decidedBy());
    } else if (event instanceof JourneyFailed failed) {
      set("step", failed.step());
      set("cause", failed.failure().getCause());
      reason(failed.failure().getReason());
      set("error", failed.failure().getErrorMessage());
      decidedBy(failed.decidedBy());
    } else {
      return false;
    }
    return true;
  }

  private boolean emitted(Event event) {
    if (event instanceof StepInfo info) {
      emitted(info.step(), info.attempt(), info.message(), info.data());
    } else if (event instanceof StepWarning warning) {
      emitted(warning.step(), warning.attempt(), warning.message(), warning.data());
    } else if (event instanceof StepError error) {
      emitted(error.step(), error.attempt(), error.message(), error.data());
    } else {
      return false;
    }
    return true;
  }

  private void set(Cell cell) {
    set(cell.column(), cell.value());
  }

  private void set(String column, Object cell) {
    if (cell != null) {
      cells.put(column, cell.toString());
    }
  }

  private void attempt(StepName step, Attempt attempt) {
    set("step", step);
    set("attempt", attempt);
  }

  private void attempted(StepName step, Attempt attempt, Reason reason) {
    attempt(step, attempt);
    reason(reason);
  }

  private void adapted(StepName step, Attempt attempt, String key, Object adapter) {
    attempt(step, attempt);
    set("key", key);
    set("adapter", adapter);
  }

  private void decided(StepName step, Attempt attempt, Object cause, DecidedBy decidedBy) {
    attempt(step, attempt);
    set("cause", cause);
    decidedBy(decidedBy);
  }

  private void reason(Reason reason) {
    if (reason == null) {
      return;
    }
    set("code", reason.code());
    carried.addAll(Stream.of(reason.message(), reason.details()).filter(Objects::nonNull).toList());
  }

  private void decidedBy(DecidedBy decidedBy) {
    set("decided by", writtenBy(decidedBy));
  }

  private void emitted(StepName step, Attempt attempt, String message, Object emitted) {
    attempt(step, attempt);
    set("message", message);
    data = emitted == null ? new Data.None() : new Data.Is(emitted);
  }

  private static String writtenBy(DecidedBy decidedBy) {
    if (decidedBy instanceof DecidingHook hook) {
      return hook.policy() + ", " + hook.hook();
    }
    return "default";
  }
}
