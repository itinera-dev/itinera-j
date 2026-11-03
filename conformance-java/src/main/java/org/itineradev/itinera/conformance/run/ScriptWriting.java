package org.itineradev.itinera.conformance.run;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.model.ScriptRow;
import org.itineradev.itinera.conformance.model.ScriptedEnding;
import org.itineradev.itinera.conformance.model.TestSupport;
import org.itineradev.itinera.executor.LocalExecutor;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.executor.testing.AttemptChoice;
import org.itineradev.itinera.executor.testing.AttemptContributing;
import org.itineradev.itinera.executor.testing.AttemptEmitting;
import org.itineradev.itinera.executor.testing.AttemptScript;
import org.itineradev.itinera.executor.testing.AttemptSettingGlobalMetadata;
import org.itineradev.itinera.executor.testing.Script;
import org.itineradev.itinera.executor.testing.Scripted;
import org.itineradev.itinera.executor.testing.ScriptedRun;
import org.itineradev.itinera.executor.testing.StepScript;
import org.itineradev.itinera.instance.WorkflowInstance;
import org.itineradev.itinera.report.testing.Level;
import org.itineradev.itinera.workflow.Sync;

/** How the runner writes a script's rows through test support, and runs it. */
final class ScriptWriting {
  private ScriptWriting() {}

  /**
   * Runs {@code instance} with the script {@code testing} holds, written through test support, on a
   * {@code LocalExecutor} given no dispatcher factory.
   *
   * @throws Refusal when {@code run} refuses the journey
   * @throws org.itineradev.itinera.testing.TestMistake when the script is mistaken
   */
  static ScriptedRun scriptedRunOf(WorkflowInstance<Sync> instance, TestSupport testing)
      throws Refusal {
    var rows = testing.script();
    if (rows == null || rows.isEmpty()) {
      throw new ModelException("no script is written");
    }
    var script = Scripted.scripted(instance);
    if (testing.ensuresAllScripted()) {
      script.ensureAllScripted();
    }
    return written(script, rows).run(new LocalExecutor());
  }

  private static StepScript written(Script script, List<ScriptRow> rows) {
    StepScript written = null;
    String step = null;
    for (var row : rows) {
      AttemptScript<StepScript> chosen;
      if (written != null && row.step().equals(step)) {
        chosen = chosen(written, row.attempt());
      } else {
        var named = written == null ? script.step(row.step()) : written.andStep(row.step());
        chosen = row.attempt() == null ? named : chosen(named, row.attempt());
      }
      written = played(chosen, row);
      step = row.step();
    }
    return written;
  }

  private static AttemptScript<StepScript> chosen(
      AttemptChoice<StepScript> choice, String attempt) {
    if (attempt == null) {
      throw new ModelException("only the first row of a step may leave its attempt unsaid");
    }
    if (attempt.equals("every")) {
      return choice.onEveryAttempt();
    }
    if (attempt.equals("remaining")) {
      return choice.onRemainingAttempts();
    }
    try {
      return choice.onAttempt(Integer.parseInt(attempt));
    } catch (NumberFormatException notWhole) {
      throw new ModelException(attempt + " is not an attempt the JVM can write");
    }
  }

  private static StepScript played(AttemptScript<StepScript> chosen, ScriptRow row) {
    if (row.ending() == ScriptedEnding.REAL_RUN) {
      return runForReal(chosen, row);
    }
    AttemptContributing<StepScript> contributing = chosen;
    for (Map.Entry<String, Object> entry : row.contributes().entrySet()) {
      contributing = contributing.willContribute(entry.getKey(), entry.getValue());
    }
    AttemptEmitting<StepScript> emitting = contributing;
    if (row.emits() != null) {
      emitting = contributing.willEmit(levelOf(row), messageOf(row));
    }
    AttemptSettingGlobalMetadata<StepScript> setting = emitting;
    for (Map.Entry<String, Object> entry : row.setsGlobalMetadata().entrySet()) {
      setting = setting.willSetGlobalMetadata(entry.getKey(), entry.getValue());
    }
    return switch (row.ending()) {
      case SUCCESS -> setting.willSucceed();
      case FAILURE -> setting.willFail(codeOf(row));
      case SKIPPED -> row.code() == null ? setting.willSkip() : setting.willSkipBecause(row.code());
      case ABNORMAL_TERMINATION ->
          setting.willTerminateAbnormally(
              row.message() == null ? Scripts.UNWRITTEN_MESSAGE : row.message());
      case REAL_RUN -> throw new ModelException("a real run plays nothing");
    };
  }

  private static StepScript runForReal(AttemptScript<StepScript> chosen, ScriptRow row) {
    if (fillsAnotherColumn(row)) {
      throw new ModelException("the real run of " + row.step() + " fills another column");
    }
    return chosen.willRunForReal();
  }

  private static boolean fillsAnotherColumn(ScriptRow row) {
    return !row.contributes().isEmpty()
        || !row.setsGlobalMetadata().isEmpty()
        || Stream.of(row.emits(), row.emitMessage(), row.code(), row.message())
            .anyMatch(Objects::nonNull);
  }

  private static Level levelOf(ScriptRow row) {
    return switch (row.emits()) {
      case STEP_INFO -> Level.INFO;
      case STEP_WARNING -> Level.WARNING;
      case STEP_ERROR -> Level.ERROR;
    };
  }

  private static String messageOf(ScriptRow row) {
    if (row.emitMessage() == null) {
      throw new ModelException("the row of " + row.step() + " emits with no message");
    }
    return row.emitMessage();
  }

  private static String codeOf(ScriptRow row) {
    if (row.code() == null) {
      throw new ModelException("the row of " + row.step() + " fails with no code");
    }
    return row.code();
  }
}
