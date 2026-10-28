package org.itineradev.itinera.conformance.model;

/**
 * When a provider fails: on {@code received} for its request of data under {@code key}, or on every
 * call when {@code key} is {@code null}.
 *
 * @param message the message it fails with
 * @param key the key of the request whose value makes it fail, or {@code null}
 * @param received the value of that request that makes it fail, or {@code null}
 */
public record ProviderFailure(String message, String key, String received) {}
