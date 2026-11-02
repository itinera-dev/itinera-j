package org.itineradev.itinera.engine

import java.lang.reflect.Parameter
import java.util.Optional
import org.itineradev.itinera.journey.DataBag
import org.itineradev.itinera.journey.Datum
import org.itineradev.itinera.journey.OutputException
import org.itineradev.itinera.journey.OutputProblem
import org.itineradev.itinera.value.Made
import org.itineradev.itinera.value.Shape
import org.itineradev.itinera.value.componentType
import org.itineradev.itinera.value.construct
import org.itineradev.itinera.value.kotlinClasses
import org.itineradev.itinera.value.requestable
import org.itineradev.itinera.value.shape

/**
 * A record or data class read as a journey's data, each component one key of the data bag: initial
 * data given as one, and a journey's output read as one.
 *
 * A component is keyed by its name unless `@Datum` names its key, and is optional as a request is:
 * of a nullable type in a Kotlin class that `itinera-kotlin` reads, or an `Optional` in any other,
 * unless `@Datum` makes it required.
 */
internal class JourneyData
private constructor(
    private val shape: Shape.Composite,
    private val components: List<KeyedComponent>,
) {
    /**
     * The entries [value] gives, in its components' order: each required component's, and each
     * optional one's that holds a value.
     */
    fun entries(value: Any): List<Pair<String, Any?>> = components.mapNotNull { component ->
        component.entry(value)
    }

    /**
     * Reads an instance from [bag], key by key in the components' order, throwing what its
     * constructor throws.
     *
     * @throws OutputException on the first key that holds no value a required component needs, or a
     *   value of another type.
     */
    fun read(bag: DataBag): Any =
        construct(shape, components.map { component -> component.read(bag) })

    companion object {
        /**
         * The journey data of [type], a record or data class.
         *
         * @throws IllegalArgumentException when [type] is neither, when two components give one
         *   key, when an `Optional` component is marked required or read without `itinera-java`, or
         *   when no value can be of a component's type.
         */
        fun of(type: ClassType): JourneyData {
            val shape = shape(type.raw)
            require(shape is Shape.Composite) {
                "Journey data is given and read as a record or a data class, each component one " +
                    "key, but ${type.raw.name} is neither. A Kotlin data class needs the " +
                    "artifact org.itinera-dev:itinera-kotlin."
            }
            val components =
                shape.components.zip(shape.constructor.parameters) { component, parameter ->
                    keyed(type, component, parameter)
                }
            val repeated = components.groupBy(KeyedComponent::key).filterValues(::isRepeated)
            require(repeated.isEmpty()) {
                "${type.raw.name} gives the keys ${repeated.keys} to more than one component."
            }
            return JourneyData(shape, components)
        }
    }
}

private fun isRepeated(components: List<KeyedComponent>): Boolean = components.size > 1

/**
 * [component] of [owner] as one key, with the `@Datum` of its constructor's [parameter], or of its
 * field, where Java puts it when a record's canonical constructor is written out.
 */
private fun keyed(owner: ClassType, component: Component, parameter: Parameter): KeyedComponent {
    val marked =
        parameter.getAnnotation(Datum::class.java)
            ?: component.field.getAnnotation(Datum::class.java)
    val name = component.field.name
    val required = marked?.required ?: false
    val type = componentType(owner, component)
    val kotlin = isKotlin(owner.raw) && kotlinClasses != null
    require(kotlin || !required || !isOptional(type)) {
        "${owner.raw.name} marks its component $name required, but it is an Optional, which is " +
            "always optional: a required component is not written as an Optional."
    }
    val form = WrittenRequest(type, required, kotlin)
    val answer =
        requireNotNull(form.answer()) {
            "${owner.raw.name} has an Optional component, which itinera reads only with the " +
                "artifact org.itinera-dev:itinera-java: add it to the application's dependencies."
        }
    val key = marked?.key?.ifEmpty { null } ?: name
    return KeyedComponent(key, component, form.required(), requestable(form.type()), answer)
}

/** A component as one key of the data bag, read as [type] and given as [answer] gives it. */
private class KeyedComponent(
    val key: String,
    private val component: Component,
    private val required: Boolean,
    private val type: ValueType,
    private val answer: Answer,
) {
    /**
     * The entry the component of [value] gives, the content of a Java `Optional`, or `null` when it
     * is optional and holds none.
     */
    fun entry(value: Any): Pair<String, Any?>? {
        val content = component.field.get(value)
        val held = if (answer == Answer.ITSELF) content else (content as Optional<*>?)?.orElse(null)
        return if (held == null && !required) null else key to held
    }

    fun read(bag: DataBag): Any? {
        val stored = bag.stored(key)
        return when {
            stored == null && required -> throw OutputException(OutputProblem.Absent(key))
            stored == null -> answer.of(null)
            else ->
                when (val receipt = Values.receive(stored, Made<Any?>(type))) {
                    is Received -> answer.of(receipt.value)
                    is WrongType -> throw otherType(receipt)
                }
        }
    }

    /** The exception for a value of another type, caused by what copying it threw, if anything. */
    private fun otherType(receipt: WrongType): OutputException =
        OutputException(OutputProblem.OtherType(key)).apply { initCause(receipt.error) }
}
