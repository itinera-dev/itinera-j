package org.itineradev.itinera.workflow;

import org.itineradev.itinera.journey.DataBagAccess;
import org.itineradev.itinera.journey.Read;
import org.itineradev.itinera.request.Got;
import org.itineradev.itinera.request.InputAdapterKind;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.value.TypeToken;

/** What {@link JavaAdapterTypesTest} refuses, written so that it compiles. */
final class JavaAdapterTypesTwins {
  Read<Long> read(DataBagAccess access) {
    return access.read("amount", TypeToken.of(Long.class));
  }

  String key(Got<InputAdapterKind> got) {
    return got.get(Requests.key());
  }
}
