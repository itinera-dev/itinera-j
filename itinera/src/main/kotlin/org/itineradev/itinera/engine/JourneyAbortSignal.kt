package org.itineradev.itinera.engine

import org.itineradev.itinera.journey.Abort

/**
 * Ends what the engine is doing once the journey is aborted, carrying the abort recorded: no
 * application can name it, and the engine catches it only where it runs the journey.
 */
internal class JourneyAbortSignal(val abort: Abort) : RuntimeException(null, null, false, false)
