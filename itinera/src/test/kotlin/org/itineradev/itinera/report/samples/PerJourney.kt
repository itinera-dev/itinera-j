@file:OptIn(ExperimentalItineraApi::class)

package org.itineradev.itinera.report.samples

import org.itineradev.itinera.ExperimentalItineraApi
import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.samples.Orders
import org.itineradev.itinera.workflow.samples.building

fun perJourney(lines: MutableList<String>): WorkflowInstance<Sync> =
    building()
        .instance(Orders("corner"))
        .reporterFor { journeyId -> Reporter { event -> lines += "$journeyId ${event.kind}" } }
        .create()
