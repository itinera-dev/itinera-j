package org.itineradev.itinera.workflow

/**
 * What an input adapter returns for the input it is asked for: [Value], which the input takes, or
 * [FromDataBag], which leaves the input to be read from the data bag, as if the step had no
 * adapter. An annotated workflow makes them with `supply` and `fromDataBag`.
 *
 * @sample org.itineradev.itinera.workflow.samples.adapters
 */
public sealed interface Supplied {
    /**
     * The value the input takes, checked against the input's type as a value from the data bag
     * would be; `null` is the value null.
     *
     * @sample org.itineradev.itinera.workflow.samples.adapters
     */
    @JvmRecord
    public data class Value(
        /** The value. */
        val value: Any?
    ) : Supplied

    /**
     * Nothing: the adapter does not supply this input, which is read from the data bag.
     *
     * @sample org.itineradev.itinera.workflow.samples.adapters
     */
    public data object FromDataBag : Supplied
}
