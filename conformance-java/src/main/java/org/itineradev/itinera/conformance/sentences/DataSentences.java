package org.itineradev.itinera.conformance.sentences;

import static org.itineradev.itinera.conformance.model.Values.value;
import static org.itineradev.itinera.conformance.sentences.Tables.cell;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.IdGenerator;
import org.itineradev.itinera.conformance.model.Model;

/**
 * The sentences that declare what the workflow instance is created with: its initial data, where
 * its journey ID comes from, and its initial global metadata.
 */
public final class DataSentences {
  private final World world;

  /** The sentences of the scenario sharing {@code world}. */
  public DataSentences(World world) {
    this.world = world;
  }

  private Model model() {
    return world.model();
  }

  @Given("the data bag is empty")
  public void theDataBagIsEmpty() {
    model().emptyDataBag();
  }

  @Given("the data bag contains:")
  public void theDataBagContains(DataTable data) {
    for (var row : data.asMaps()) {
      model().contains(cell(row, "key"), value(cell(row, "value")));
    }
  }

  @Given("the workflow has no ID generator")
  public void noIdGenerator() {
    model().generateIdsWith(new IdGenerator.None());
  }

  @Given("the workflow's ID generator returns {string}")
  public void idGeneratorReturns(String id) {
    model().generateIdsWith(new IdGenerator.Returns(id));
  }

  @Given("the workflow instance is created with the global metadata:")
  public void createdWithTheGlobalMetadata(DataTable metadata) {
    for (var row : metadata.asMaps()) {
      model().globalMetadata(cell(row, "key"), value(cell(row, "value")));
    }
  }
}
