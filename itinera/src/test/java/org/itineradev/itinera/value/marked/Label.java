package org.itineradev.itinera.value.marked;

/** A record whose package is null-marked, so its component refuses {@code null}. */
public record Label(String text) {}
