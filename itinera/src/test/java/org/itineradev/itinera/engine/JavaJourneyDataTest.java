package org.itineradev.itinera.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.itineradev.itinera.instance.InstanceBuilder;
import org.itineradev.itinera.journey.DataBag;
import org.itineradev.itinera.journey.DataBags;
import org.itineradev.itinera.journey.Datum;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.journey.JourneyResult;
import org.itineradev.itinera.journey.JourneyStatus;
import org.itineradev.itinera.journey.OutputException;
import org.itineradev.itinera.journey.OutputProblem;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.StepDescriptor;
import org.itineradev.itinera.workflow.WorkflowDescriptor;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

final class JavaJourneyDataTest {
  record Receipt(String order, long total) {}

  record Shipped(@Datum(key = "tracking") String code) {}

  record Twice(String order, @Datum(key = "order") String again) {}

  record Maybe(Optional<String> note) {}

  record RequiredMaybe(@Datum(required = true) Optional<String> note) {}

  record Written(@Datum(key = "tracking") String code) {
    Written(String code) {
      this.code = code;
    }
  }

  record Noted(String order, @Nullable String note) {}

  static final class Shop {}

  private static final JourneyId ID = JourneyId.of("order-42");

  private static JourneyResult succeeded(DataBag bag) {
    return new JourneyResult(ID, new JourneyStatus.Succeeded(bag));
  }

  private static Map<String, @Nullable Object> given(Object data) {
    var workflow =
        WorkflowDescriptor.builder(Shop.class, "shop")
            .step(StepDescriptor.builder("pack", got -> Outcome::success).build())
            .build();
    return InstanceBuilder.of(workflow, new Shop()).initialData(data).create().getInitialData();
  }

  @Test
  void anOutputReadsEachComponentUnderItsNameAndLeavesOtherKeysUnread() {
    var bag = DataBags.of(Map.of("order", "42", "total", 40L, "note", "x"));
    assertEquals(new Receipt("42", 40L), succeeded(bag).output(TypeToken.of(Receipt.class)));
  }

  @Test
  void aDatumNamesItsComponentsKey() {
    var bag = DataBags.of("tracking", "T-1");
    assertEquals(new Shipped("T-1"), succeeded(bag).output(TypeToken.of(Shipped.class)));
  }

  @Test
  void aDatumIsReadFromTheFieldWhenTheCanonicalConstructorIsWrittenOut() {
    var bag = DataBags.of("tracking", "T-1");
    assertEquals(new Written("T-1"), succeeded(bag).output(TypeToken.of(Written.class)));
  }

  static final List<Arguments> problems =
      List.of(
          argumentSet(
              "a required key that holds nothing",
              DataBags.of("order", "42"),
              new OutputProblem.Absent("total")),
          argumentSet(
              "a key holding a value of another type",
              DataBags.of(Map.of("order", "42", "total", 40)),
              new OutputProblem.OtherType("total")));

  @ParameterizedTest
  @FieldSource("problems")
  void anOutputThatCannotBeReadSaysWhy(DataBag bag, OutputProblem problem) {
    var thrown =
        assertThrows(
            OutputException.class, () -> succeeded(bag).output(TypeToken.of(Receipt.class)));
    assertEquals(problem, thrown.getProblem());
  }

  static final List<Arguments> refused =
      List.of(
          argumentSet("a class that is not a record", String.class),
          argumentSet("two components giving one key", Twice.class),
          argumentSet("an Optional component without itinera-java", Maybe.class),
          argumentSet("an Optional component marked required", RequiredMaybe.class));

  @ParameterizedTest
  @FieldSource("refused")
  void anOutputIsReadOnlyAsARecordWhoseComponentsGiveOneKeyEach(Class<?> type) {
    var bag = DataBags.of("order", "42");
    assertThrows(IllegalArgumentException.class, () -> succeeded(bag).output(TypeToken.of(type)));
  }

  @Test
  void aRecordGivesItsComponentsAsInitialDataInTheirOrder() {
    assertEquals(List.of("order", "total"), List.copyOf(given(new Receipt("42", 40L)).keySet()));
  }

  @Test
  void aDatumNamesTheKeyItsComponentIsGivenUnder() {
    assertEquals(Map.of("tracking", "T-1"), given(new Shipped("T-1")));
  }

  @Test
  void aJavaComponentThatIsNotAnOptionalIsRequiredSoItsNullIsGiven() {
    var expected = new HashMap<String, @Nullable Object>();
    expected.put("order", "42");
    expected.put("note", null);
    assertEquals(expected, given(new Noted("42", null)));
  }

  @Test
  void aRequiredComponentWhoseKeyHoldsNullReadsNull() {
    var data = new HashMap<String, @Nullable Object>();
    data.put("order", "42");
    data.put("note", null);
    var bag = DataBags.of(data);
    assertEquals(new Noted("42", null), succeeded(bag).output(TypeToken.of(Noted.class)));
  }

  @Test
  void initialDataGivenAsARecordWithTwoComponentsForOneKeyIsRefused() {
    assertThrows(IllegalArgumentException.class, () -> given(new Twice("42", "43")));
  }
}
