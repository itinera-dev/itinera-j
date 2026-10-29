package org.itineradev.itinera.workflow.checker.fixtures

import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.JourneyId
import org.itineradev.itinera.policy.FromStep
import org.itineradev.itinera.service.Provided
import org.itineradev.itinera.service.Provider
import org.itineradev.itinera.service.Service
import org.itineradev.itinera.service.async.AsyncProviderReporter
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.step.StepName
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.Key
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.async.ItineraAsyncWorkflow

/** Service providers' mistakes in Kotlin. */
class KotlinProviders private constructor() {
    interface PaymentGateway {
        fun take(amount: Long): String
    }

    @Step
    class Pay : ItineraStep {
        fun run(): Outcome = succeed()
    }

    @Step
    class AsyncUnpack : ItineraStep {
        suspend fun run(): Outcome = succeed()
    }

    @WithStep(Pay::class, name = "pay")
    class NamingProvider : ItineraWorkflow {
        @Provider fun receipts(step: StepName): Provided<String> = provide("receipt of $step")
    }

    @WithStep(Pay::class, name = "pay")
    class MisattachedProvider : ItineraWorkflow {
        @Provider(steps = ["ship"]) fun labels(): Provided<String> = noInstance()
    }

    @WithStep(Pay::class, name = "pay")
    class SuspendingProvider : ItineraWorkflow {
        @Provider suspend fun labels(): Provided<String> = noInstance()
    }

    @WithStep(AsyncUnpack::class, name = "unpack")
    class PlainAsyncProvider : ItineraAsyncWorkflow {
        @Provider fun labels(): Provided<String> = noInstance()
    }

    @WithStep(Pay::class, name = "pay")
    class EmptyStepProvider : ItineraWorkflow {
        @Provider(steps = [""]) fun labels(): Provided<String> = noInstance()
    }

    @WithStep(Pay::class, name = "pay")
    class StringProvider : ItineraWorkflow {
        @Provider fun labels(journey: JourneyId): String = "label $journey"
    }

    @WithStep(Pay::class, name = "pay")
    class MaybeProvider : ItineraWorkflow {
        @Provider fun labels(journey: JourneyId): Provided<String>? = provide("label $journey")
    }

    @WithStep(Pay::class, name = "pay")
    class GenericProvider : ItineraWorkflow {
        @Provider fun <T : Any> labels(): Provided<T> = noInstance()
    }

    @WithStep(Pay::class, name = "pay")
    class FromStepProvider : ItineraWorkflow {
        @Provider fun labels(@FromStep charged: Long): Provided<String> = provide("$charged")
    }

    @WithStep(Pay::class, name = "pay")
    class KeyProvider : ItineraWorkflow {
        @Provider fun labels(@Key key: String): Provided<String> = provide(key)
    }

    @WithStep(Pay::class, name = "pay")
    class ServiceProvider : ItineraWorkflow {
        @Provider
        fun labels(@Service payments: PaymentGateway): Provided<String> = provide(payments.take(1))
    }

    @WithStep(Pay::class, name = "pay")
    class ContributingProvider : ItineraWorkflow {
        @Provider fun labels(contributor: Contributor): Provided<String> = provide("$contributor")
    }

    @WithStep(Pay::class, name = "pay")
    class AsyncReporterProvider : ItineraWorkflow {
        @Provider
        fun labels(reporter: AsyncProviderReporter): Provided<String> = provide("$reporter")
    }

    @WithStep(Pay::class, name = "pay")
    class UnaskingProvider : ItineraWorkflow {
        @Provider fun labels(retries: Int): Provided<String> = provide("$retries")
    }

    @Step
    class Counting : ItineraStep {
        fun run(@Service("count") count: Int): Outcome = if (count > 0) succeed() else fail("none")
    }

    @WithStep(Counting::class, name = "count")
    class WrongCount : ItineraWorkflow {
        @Provider(key = "count") fun count(): Provided<Long> = provide(1L)
    }

    @WithStep(Counting::class, name = "count")
    class StarProvider : ItineraWorkflow {
        @Provider(key = "count") fun count(): Provided<*> = noInstance<Int>()
    }

    @WithStep(Pay::class, name = "pay")
    class ArrayProvider : ItineraWorkflow {
        @Provider fun labels(): Provided<Array<String>> = noInstance()
    }

    @WithStep(Pay::class, name = "pay")
    class GenericArrayProvider : ItineraWorkflow {
        @Provider fun <T : Any> labels(): Provided<Array<T>> = noInstance()
    }

    @WithStep(Pay::class, name = "pay")
    class AliasProvider : ItineraWorkflow {
        @Provider fun labels(): Labels = noInstance()
    }

    @WithStep(Pay::class, name = "pay")
    class GenericAliasProvider : ItineraWorkflow {
        @Provider fun labels(): Served<String> = noInstance()
    }

    @Step
    class Labelling : ItineraStep {
        fun run(@Service("labels") labels: Array<CharSequence>): Outcome =
            if (labels.isEmpty()) fail("unlabelled") else succeed()
    }

    @WithStep(Labelling::class, name = "label")
    class BoundArrayProvider : ItineraWorkflow {
        @Provider(key = "labels") fun <T : CharSequence> labels(): Provided<Array<T>> = noInstance()
    }

    @Step
    class Numbering : ItineraStep {
        fun run(@Service("numbers") numbers: Array<Number>): Outcome =
            if (numbers.isEmpty()) fail("none") else succeed()
    }

    @WithStep(Numbering::class, name = "number")
    class ClassBoundArrayProvider : ItineraWorkflow {
        @Provider(key = "numbers")
        fun <T> numbers(): Provided<Array<T>> where T : Comparable<T>, T : Number = noInstance()
    }
}

/** Labels, served by a provider whose return type an alias names. */
typealias Labels = Provided<String>

/** What a provider serves, named by an alias with a type parameter. */
typealias Served<T> = Provided<T>
