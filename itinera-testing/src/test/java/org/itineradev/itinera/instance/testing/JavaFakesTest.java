package org.itineradev.itinera.instance.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.itineradev.itinera.executor.LocalExecutor;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.instance.testing.samples.AcceptingGateway;
import org.itineradev.itinera.instance.testing.samples.Gateway;
import org.itineradev.itinera.instance.testing.samples.Payments;
import org.itineradev.itinera.service.Provided;
import org.itineradev.itinera.testing.Mistake;
import org.itineradev.itinera.testing.TestMistake;
import org.junit.jupiter.api.Test;

final class JavaFakesTest {
  @Test
  void aJavaFakeReplacesTheProvidersFunction() throws Refusal {
    var instance =
        TestInstanceBuilder.of(new Payments())
            .fakeProvider(
                "gateway", Gateway.class, () -> new Provided.Instance<>(new AcceptingGateway()))
            .create();
    assertTrue(new LocalExecutor().run(instance).isSucceeded());
  }

  @Test
  void aJavaFakeOfAnotherServiceIsAMistake() {
    var builder = TestInstanceBuilder.of(new Payments());
    var mistake =
        assertThrows(
            TestMistake.class,
            () -> builder.fakeProvider("gateway", String.class, Provided::noInstance));
    assertEquals(List.of(new Mistake.AnotherService("gateway")), mistake.getProblems());
  }
}
