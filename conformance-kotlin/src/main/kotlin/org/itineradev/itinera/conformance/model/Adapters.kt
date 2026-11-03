package org.itineradev.itinera.conformance.model

/** A request an input adapter declares. */
internal sealed interface AdapterRequest {
    /** A required request for data from the workflow, read from the data bag. */
    data class FromWorkflow(val key: String, val type: ValueType) : AdapterRequest

    /** Read access to the data bag. */
    data object DataBag : AdapterRequest
}

/** What an input adapter does when it is called for one input. */
internal sealed interface AdapterAnswer {
    /** Supplies this value. */
    data class Returns(val value: Any?) : AdapterAnswer

    /** Leaves the input to the data bag. */
    data object Nothing : AdapterAnswer

    /** Fails with this message. */
    data class Fails(val message: String) : AdapterAnswer

    /** Supplies what is not a value. */
    data object NonValue : AdapterAnswer

    /** Supplies the name of the step being built. */
    data object ItsStepName : AdapterAnswer

    /**
     * Reads [key] as [type] through its access to the data bag: through the access it kept when
     * [kept], or else through the access of this call. It supplies the value it found, or leaves
     * the input to the data bag.
     */
    data class Reads(val key: String, val type: ValueType, val kept: Boolean = false) :
        AdapterAnswer
}

/**
 * An input adapter the workflow declares, attached to its steps, each as often as named, with what
 * it requests in the order written and what it does for each input it is scripted for. For any
 * other input it leaves the input to the data bag.
 */
internal class AdapterModel(val name: String, val steps: List<String>) {
    /** Whether the adapter runs asynchronously. */
    var isAsynchronous = false

    val requests = mutableListOf<AdapterRequest>()

    private val answers = mutableMapOf<Pair<String?, String>, AdapterAnswer>()

    /** The step whose call's access to the data bag the adapter keeps, once stated. */
    var keepsAccessOf: String? = null
        private set

    /** Says what the adapter does for [input], when called for [step] or else for any step. */
    fun answers(input: String, answer: AdapterAnswer, step: String? = null) {
        if (step to input in answers) {
            throw ModelException("what the adapter \"$name\" does for \"$input\" is stated twice")
        }
        answers[step to input] = answer
    }

    /** What the adapter does for [input] when called for [step]. */
    fun answerFor(step: String, input: String): AdapterAnswer =
        answers[step to input] ?: answers[null to input] ?: AdapterAnswer.Nothing

    /** Says the adapter keeps the access to the data bag of its calls for [step], once. */
    fun keepsAccessOf(step: String) {
        if (keepsAccessOf != null) {
            throw ModelException("which access the adapter \"$name\" keeps is stated twice")
        }
        keepsAccessOf = step
    }
}
