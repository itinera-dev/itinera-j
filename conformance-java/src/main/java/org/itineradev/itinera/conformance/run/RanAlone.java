package org.itineradev.itinera.conformance.run;

import java.util.List;
import java.util.Map;
import org.itineradev.itinera.journey.testing.Contributions;
import org.itineradev.itinera.report.testing.Emitted;

/**
 * What a step or a hook run alone, outside any journey, was lent and did.
 *
 * @param called what the sentences name as run alone
 * @param recorder the recorder that lent its reporter, {@code step} or {@code hook}
 * @param contributions the contributions recorder that lent its contributor
 * @param emitted what that recorder kept of the events
 * @param globalMetadata what that recorder kept of the global metadata, in the order set
 * @param calls the calls made on the reporter, in order
 */
public record RanAlone(
    String called,
    String recorder,
    Contributions contributions,
    List<Emitted> emitted,
    Map<String, Object> globalMetadata,
    List<Call> calls) {}
