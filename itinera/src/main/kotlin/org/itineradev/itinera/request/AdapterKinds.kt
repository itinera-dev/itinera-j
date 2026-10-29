package org.itineradev.itinera.request

/**
 * An input adapter, as the kind of part that requests: the name of the step being built, the key
 * being resolved, data from the workflow, the journey ID and read access to the data bag. It
 * requests no role and no contributor, since it only supplies values.
 *
 * @sample org.itineradev.itinera.workflow.samples.adapters
 */
public interface InputAdapterKind :
    StepNameKind, KeyKind, WorkflowDataKind, JourneyIdKind, DataBagAccessKind

/**
 * The kinds of part that may request the key of the input being resolved: input adapters.
 *
 * @sample org.itineradev.itinera.workflow.samples.adapters
 */
public interface KeyKind

/**
 * The kinds of part that may request read access to the data bag: input adapters.
 *
 * @sample org.itineradev.itinera.workflow.samples.adapters
 */
public interface DataBagAccessKind
