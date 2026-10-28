package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.run.Verdicts.holds;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.workflow.ListedService;
import org.itineradev.itinera.workflow.ListedStep;

/** The sentences that list the workflow and check its listing. */
public final class ListingSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public ListingSentences(World world) {
    this.world = world;
  }

  @When("the workflow is listed")
  public void isListed() {
    world.admit();
  }

  /** Compares only the columns the table has, each cell as the cases write it. */
  @Then("the listing is:")
  public void theListingIs(DataTable table) {
    var columns = table.row(0);
    var expected = table.asMaps().stream().map(ListingSentences::written).toList();
    var found = world.listing().stream().map(step -> rowOf(step, columns)).toList();
    holds(found.equals(expected), "the listing " + expected, found.toString());
  }

  /** A row as the table writes it, an empty cell as an empty string. */
  private static Map<String, String> written(Map<String, String> row) {
    return row.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, ListingSentences::orEmpty));
  }

  private static String orEmpty(Map.Entry<String, String> cell) {
    return Objects.requireNonNullElse(cell.getValue(), "");
  }

  private static Map<String, String> rowOf(ListedStep step, List<String> columns) {
    return columns.stream()
        .collect(Collectors.toMap(Function.identity(), column -> cellOf(step, column)));
  }

  private static String cellOf(ListedStep step, String column) {
    var rescue = step.rescue();
    return switch (column) {
      case "step" -> step.step().toString();
      case "position" -> step.position().toString();
      case "policies" -> joined(step.policies());
      case "adapter" -> Objects.toString(step.adapter(), "");
      case "services" -> written(step.services());
      case "rescue" -> rescue == null ? "" : rescue.step().toString();
      case "rescue adapter" -> rescue == null ? "" : Objects.toString(rescue.adapter(), "");
      case "rescue services" -> rescue == null ? "" : written(rescue.services());
      default -> throw new ModelException("a listing has no column " + column);
    };
  }

  private static String joined(List<?> names) {
    return names.stream().map(Object::toString).collect(Collectors.joining(", "));
  }

  /** Services as the cases write them: {@code <key>: <provider>}, separated by commas. */
  private static String written(List<ListedService> services) {
    return services.stream().map(ListingSentences::written).collect(Collectors.joining(", "));
  }

  private static String written(ListedService service) {
    return service.key() + ": " + service.provider();
  }
}
