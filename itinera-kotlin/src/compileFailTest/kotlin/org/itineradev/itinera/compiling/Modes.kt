package org.itineradev.itinera.compiling

/** The start of a snippet that uses the parts of both modes. */
internal const val MODES =
    """
    import org.itineradev.itinera.executor.LocalExecutor
    import org.itineradev.itinera.executor.async.AsyncLocalExecutor
    import org.itineradev.itinera.instance.InstanceBuilder
    import org.itineradev.itinera.instance.WorkflowInstance
    import org.itineradev.itinera.instance.async.AsyncInstanceBuilder
    import org.itineradev.itinera.report.*
    import org.itineradev.itinera.report.async.*
    import org.itineradev.itinera.request.*
    import org.itineradev.itinera.request.async.*
    import org.itineradev.itinera.step.async.AsyncStepRun
    import org.itineradev.itinera.workflow.*
    import org.itineradev.itinera.workflow.async.asyncStep
    """
