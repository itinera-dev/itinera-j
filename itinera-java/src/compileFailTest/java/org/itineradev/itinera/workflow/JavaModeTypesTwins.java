package org.itineradev.itinera.workflow;

import org.itineradev.itinera.service.ProviderDescriptor;

/** What {@link JavaModeTypesTest} refuses, written so that it compiles. */
final class JavaModeTypesTwins {
  void add(
      WorkflowBuilder<JavaModeTypesTwins, Sync> shop,
      StepDescriptor<Sync> charge,
      InputAdapterDescriptor<JavaModeTypesTwins, Sync> pricing,
      ProviderDescriptor<JavaModeTypesTwins, Sync> payments) {
    shop.step(charge).inputAdapter(pricing).provider(payments);
  }
}
