package org.itineradev.itinera.value

/** Thrown inside a copy when what it copies is not a value; no custom code failed. */
internal class NotAValueException : RuntimeException(null, null, false, false)
