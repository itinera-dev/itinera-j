package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.run.Verdicts.holds;
import static org.itineradev.itinera.conformance.sentences.Tables.cell;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.util.List;
import java.util.Map;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.run.Admission;
import org.itineradev.itinera.conformance.run.Ended;
import org.itineradev.itinera.conformance.run.Unmet;
import org.itineradev.itinera.executor.Refused;
import org.itineradev.itinera.workflow.Violation;

/** The sentences that check how the workflow was admitted. */
public final class AdmissionSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public AdmissionSentences(World world) {
    this.world = world;
  }

  @Then("admission is refused with the violations:")
  public void admissionIsRefused(DataTable table) {
    var expected = table.asMaps().stream().map(AdmissionSentences::violationOf).sorted().toList();
    var found = refusedWith().stream().map(AdmissionSentences::nameOf).sorted().toList();
    holds(found.equals(expected), "the violations " + expected, found.toString());
  }

  /**
   * The violations the builder refused the workflow with, or {@code run} refused its instance with.
   */
  private List<Violation> refusedWith() {
    if (world.admission() instanceof Admission.Refused refused) {
      return refused.violations().getViolations();
    }
    var journeys = world.journeys();
    if (!journeys.isEmpty()
        && journeys.get(journeys.size() - 1).ended() instanceof Ended.Refused refused
        && refused.refusal().getRefused() instanceof Refused.Inadmissible inadmissible) {
      return inadmissible.violations();
    }
    throw new Unmet("expected admission to be refused; it was admitted");
  }

  private static String violationOf(Map<String, String> row) {
    return cell(row, "violation");
  }

  private static String nameOf(Violation violation) {
    return String.valueOf(violation.getNamed());
  }
}
