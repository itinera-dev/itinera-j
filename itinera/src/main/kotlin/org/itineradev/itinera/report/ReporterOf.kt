package org.itineradev.itinera.report

import org.itineradev.itinera.workflow.Mode

/**
 * A reporter of a workflow of mode [M]: [Reporter] for a synchronous workflow, and `AsyncReporter`
 * from `itinera-kotlin` for an asynchronous one. A workflow instance holds its reporters as
 * reporters of its own mode, so one of the other mode cannot join it; the executor checks them
 * again before the journey.
 *
 * @param M the mode of the workflows the reporter can report.
 * @sample org.itineradev.itinera.report.samples.printer
 */
public interface ReporterOf<M : Mode>
