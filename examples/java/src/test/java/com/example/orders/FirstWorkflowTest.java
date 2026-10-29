package com.example.orders;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.orders.FirstWorkflow.Orders;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.itineradev.itinera.executor.LocalExecutor;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.report.testing.RecordingReporter;
import org.junit.jupiter.api.Test;

final class FirstWorkflowTest {
  @Test
  void theFirstWorkflowRunsAsItsChecksExpect() throws Refusal {
    FirstWorkflow.main(new String[0]);
  }

  @Test
  void aChargeOfNothingIsAttemptedThreeTimesBeforeTheJourneyFails() throws Refusal {
    var recording = new RecordingReporter();
    new LocalExecutor()
        .run(new Orders().instance().data("amount", 0L).reporter(recording).create());
    assertEquals(
        List.of(
            "charge attempt 1 failed: nothing to charge",
            "charge retrying",
            "charge attempt 2 failed: nothing to charge",
            "charge retrying",
            "charge attempt 3 failed: nothing to charge",
            "charge given up: retries exhausted",
            "journey failed at charge: nothing to charge"),
        recording.getPath());
  }

  @Test
  void theReadmeShowsTheJavaExampleAsItIs() throws IOException {
    var readme = Files.readString(Path.of(System.getProperty("example.readme")));
    var shown = Pattern.compile("### In Java\n\n```java\n(.*?)```", Pattern.DOTALL).matcher(readme);
    assertEquals(
        Files.readString(Path.of(System.getProperty("example.source"))),
        shown.find() ? shown.group(1) : null);
  }
}
