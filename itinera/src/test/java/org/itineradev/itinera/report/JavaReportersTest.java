package org.itineradev.itinera.report;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.itineradev.itinera.event.Event;
import org.junit.jupiter.api.Test;

final class JavaReportersTest {
  static final class Printer implements Reporter {
    final List<String> lines = new ArrayList<>();

    @Override
    public void report(Event event) {
      lines.add(event.getKind().toString());
    }
  }

  static final class RecordingDispatcher implements Dispatcher {
    final List<String> lines = new ArrayList<>();

    @Override
    public void add(Reporter reporter) {
      lines.add("added");
    }

    @Override
    public void dispatch(Event event) {
      lines.add(event.getKind().toString());
    }

    @Override
    public void setGlobalMetadata(String key, Object value) {
      lines.add(key + "=" + value);
    }
  }

  @Test
  void aJavaClassAcceptsGlobalMetadataWithoutWritingTheOperation() {
    var printer = new Printer();
    assertDoesNotThrow(() -> printer.setGlobalMetadata("tenant", "acme"));
  }

  @Test
  void theDefaultDispatcherDeliversToAJavaLambdaAndAJavaClassInOrder() {
    var lines = new ArrayList<String>();
    var printer = new Printer();
    Dispatcher dispatcher = new DefaultDispatcherFactory().create();
    dispatcher.add(event -> lines.add("lambda"));
    dispatcher.add(printer);
    dispatcher.dispatch(Fixtures.started);
    assertEquals(List.of("lambda"), lines);
    assertEquals(List.of("journey_started"), printer.lines);
  }

  @Test
  void theDefaultDispatcherPassesGlobalMetadataToAJavaReporter() {
    var lines = new ArrayList<String>();
    Dispatcher dispatcher = new DefaultDispatcher();
    dispatcher.add(
        new Reporter() {
          @Override
          public void report(Event event) {}

          @Override
          public void setGlobalMetadata(String key, Object value) {
            lines.add(key + "=" + value);
          }
        });
    dispatcher.add(new DebugReporter());
    dispatcher.setGlobalMetadata("tenant", "acme");
    assertEquals(List.of("tenant=acme"), lines);
  }

  @Test
  void aJavaClassIsADispatcher() {
    var recording = new RecordingDispatcher();
    Dispatcher dispatcher = recording;
    dispatcher.add(new DebugReporter());
    dispatcher.dispatch(Fixtures.started);
    dispatcher.setGlobalMetadata("tenant", "acme");
    assertEquals(List.of("added", "journey_started", "tenant=acme"), recording.lines);
  }

  @Test
  void aConstructorReferenceIsADispatcherFactory() {
    DispatcherFactory factory = DefaultDispatcher::new;
    assertDoesNotThrow(factory::create);
  }
}
