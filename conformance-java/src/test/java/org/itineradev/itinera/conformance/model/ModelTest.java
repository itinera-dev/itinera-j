package org.itineradev.itinera.conformance.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ModelTest {
  private static Model orders() {
    var model = new Model();
    model.declare("orders", List.of("charge", "ship"));
    return model;
  }

  @Test
  void aWorkflowIsDeclaredOnce() {
    var model = orders();
    assertThrows(ModelException.class, () -> model.declare("refunds", List.of("refund")));
  }

  @Test
  void aSentenceAboutAStepNeedsTheWorkflowToHaveIt() {
    var model = orders();
    assertThrows(ModelException.class, () -> model.step("refund"));
  }

  @Test
  void aSentenceAboutTheWorkflowNeedsOneDeclared() {
    var model = new Model();
    assertThrows(ModelException.class, () -> model.step("charge"));
  }

  @Test
  void twoStepsOfOneNameShareOneScript() {
    var model = new Model();
    model.declare("orders", List.of("charge", "charge"));
    assertEquals(List.of("charge"), List.copyOf(model.workflow().scripts().keySet()));
  }

  @Test
  void aRescueStepIsScriptedUnderTheStepsNameWithRescue() {
    var model = orders();
    model.rescue("charge");
    assertTrue(model.step("charge").hasRescueStep());
    assertTrue(model.step("charge_rescue").isRescueStep());
  }

  @Test
  void aSentenceAboutAProviderNeedsExactlyOneOfItsNameDeclared() {
    var model = orders();
    model.workflow().provide("stock", "stock", ServiceType.LABELLER, ProviderScope.JOURNEY);
    assertThrows(ModelException.class, () -> model.workflow().provider("payments"));
    model.workflow().provide("stock", "labels", ServiceType.LABELLER, ProviderScope.ATTEMPT);
    assertThrows(ModelException.class, () -> model.workflow().provider("stock"));
  }

  @Test
  void aSentenceAboutAnInputAdapterNeedsExactlyOneOfItsNameDeclared() {
    var model = orders();
    model.workflow().adapt("pricing", List.of("charge"));
    assertThrows(ModelException.class, () -> model.workflow().adapter("discounts"));
    model.workflow().adapt("pricing", List.of("charge"));
    assertThrows(ModelException.class, () -> model.workflow().adapter("pricing"));
  }

  @Test
  void theStepsAProviderServesAreStatedOnce() {
    var model = orders();
    model.workflow().provide("stock", "stock", ServiceType.LABELLER, ProviderScope.JOURNEY);
    model.workflow().provider("stock").servesOnly(List.of("charge"));
    var provider = model.workflow().provider("stock");
    var ship = List.of("ship");
    assertThrows(ModelException.class, () -> provider.servesOnly(ship));
  }

  @Test
  void aCaseNamesOnlyTheServiceTypesOfTheCases() {
    assertThrows(ModelException.class, () -> ServiceType.named("printer"));
  }

  @Test
  void aRescueStepSharesTheScriptOfAStepOfTheSameName() {
    var model = new Model();
    model.declare("orders", List.of("charge", "charge_rescue"));
    var step = model.step("charge_rescue");
    model.rescue("charge");
    assertSame(step, model.step("charge_rescue"));
    assertTrue(step.isRescueStep());
  }

  @Test
  void initialDataHoldsEachKeyOnce() {
    var model = orders();
    model.contains("amount", 10L);
    assertThrows(ModelException.class, () -> model.contains("amount", 20L));
  }

  @Test
  void initialDataCannotFollowAnEmptyDataBag() {
    var model = orders();
    model.emptyDataBag();
    assertThrows(ModelException.class, () -> model.contains("amount", 10L));
  }

  @Test
  void anEmptyDataBagCannotFollowInitialData() {
    var model = orders();
    model.contains("amount", 10L);
    assertThrows(ModelException.class, model::emptyDataBag);
  }

  @Test
  void globalMetadataKeepsEveryEntryInTheOrderSet() {
    var model = orders();
    model.globalMetadata("tenant", "acme");
    model.globalMetadata("tenant", "globex");
    assertEquals(
        List.of(
            new SimpleImmutableEntry<>("tenant", "acme"),
            new SimpleImmutableEntry<>("tenant", "globex")),
        model.initialGlobalMetadata());
  }

  @Test
  void whatTheExecutorDispatchesWithIsStatedOnce() {
    var model = orders();
    model.dispatchWith(new Dispatching.Default());
    assertThrows(ModelException.class, () -> model.dispatchWith(new Dispatching.FactoryFails()));
  }

  @Test
  void howAReporterFailsIsStatedOnce() {
    var model = orders();
    model.fails("audit", new ReporterFailure.OnFirstEvent());
    assertThrows(
        ModelException.class,
        () -> model.fails("audit", new ReporterFailure.OnGlobalMetadata("tenant")));
  }

  @Test
  void aStepsAttemptsAreNumberedFromOneInOrder() {
    var step = orders().step("charge");
    var attempts =
        List.of(new ScriptedAttempt(2, Ending.FAILURE), new ScriptedAttempt(1, Ending.SUCCESS));
    assertThrows(ModelException.class, () -> step.attempts("charge", attempts));
  }

  @Test
  void aStepsAttemptsAreStatedOnce() {
    var step = orders().step("charge");
    step.attempts("charge", List.of(new ScriptedAttempt(1, Ending.SUCCESS)));
    var again = List.of(new ScriptedAttempt(1, Ending.FAILURE));
    assertThrows(ModelException.class, () -> step.attempts("charge", again));
  }

  @Test
  void aRetryBudgetIsNeverNegative() {
    var step = orders().step("charge");
    assertThrows(ModelException.class, () -> step.allows("charge", -1));
  }

  @Test
  void aRetryBudgetIsStatedOnce() {
    var step = orders().step("charge");
    step.allows("charge", 2);
    assertThrows(ModelException.class, () -> step.allows("charge", 3));
  }

  @Test
  void anOutcomeTheCasesDoNotNameIsAnErrorInTheCase() {
    assertThrows(ModelException.class, () -> Ending.named("aborted"));
  }
}
