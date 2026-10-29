package org.itineradev.itinera.workflow.checker.fixtures

import kotlin.properties.Delegates
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.WithStep

/** Which Kotlin types a value can be of, as steps request them. */
class KotlinValues private constructor() {
    data class Line(val product: String, val quantity: Long, val parts: List<String>)

    data class Box<T>(val content: T)

    data object Empty

    enum class Colour {
        RED,
        BLUE,
    }

    data class Counter(var count: Long)

    data class Tagged(val name: String) {
        val shout: String = name.uppercase()
    }

    data class Lazy(val name: String) {
        val upper by lazy { name.uppercase() }
    }

    data object Tally {
        var count by Delegates.notNull<Long>()
    }

    @JvmInline value class OrderId(val id: String)

    data class Order(val id: OrderId)

    class Plain(val name: String)

    object Settings {
        var verbose: Boolean = false
    }

    sealed interface Shape

    @Step
    class Refused<T>(
        @Input("counter") private val counter: Counter,
        @Input("tagged") private val tagged: Tagged,
        @Input("lazy") private val lazy: Lazy,
        @Input("order") private val order: Order,
        @Input("plain") private val plain: Plain,
        @Input("settings") private val settings: Settings,
        @Input("by number") private val byNumber: Map<Int, String>,
        @Input("generic") private val generic: T,
        @Input("names") private val names: Array<String>,
        @Input("numbers") private val numbers: LongArray,
        @Input("lines") private val lines: Set<Array<Line>>,
        @Input("shape") private val shape: Shape,
        @Input("thread") private val thread: Thread,
        @Input("weights") private val weights: Map<Line, Double>,
        @Input("by key") private val byKey: ByKey<Long>,
    ) : ItineraStep {
        fun run(): Outcome = succeed()
    }

    @WithStep(Refused::class, name = "refused") class Unvalued : ItineraWorkflow

    @Step
    class Accepted(
        @Input("lines") private val lines: List<Line>,
        @Input("mutable") private val mutable: MutableList<Line>,
        @Input("names") private val names: Map<String, List<String>>,
        @Input("boxes") private val boxes: List<Box<Long>>,
        @Input("empty") private val empty: Empty,
        @Input("colour") private val colour: Colour,
        @Input("anything") private val anything: Any,
        @Input("number") private val number: Number,
        @Input("pair") private val pair: Pair<String, Long>,
        @Input("star") private val star: List<*>,
        @Input("char sequence") private val chars: Map<CharSequence, Int>,
        @Input("tally") private val tally: Tally,
    ) : ItineraStep {
        fun run(): Outcome = succeed()
    }

    @WithStep(Accepted::class, name = "accepted") class Valued : ItineraWorkflow
}

/** Strings by keys that may be absent, named by an alias that uses its parameter as nullable. */
typealias ByKey<K> = Map<K?, String>
