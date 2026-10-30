package org.itineradev.itinera.value.kotlin

import java.time.temporal.ChronoUnit

data class Parcel(val weight: Long, val items: MutableList<String>)

data class Box<T>(val content: T)

data class Note(val text: String, val remark: String?)

/** A data class whose copies fail once [Rebuilds.fail] is set. */
data class Ticket(val code: String) {
    init {
        check(!Rebuilds.fail) { "no more tickets" }
    }
}

object Rebuilds {
    var fail = false
}

/** A data class that cannot be hashed, so a set holding it cannot be copied. */
data class Fragile(val name: String) {
    @Suppress("ExceptionRaisedInUnexpectedLocation")
    override fun hashCode(): Int = throw IllegalStateException("cannot hash $name")

    override fun equals(other: Any?): Boolean = other is Fragile && other.name == name
}

data class Counter(var count: Long)

data class Tagged(val name: String) {
    val tag = name.uppercase()
}

@JvmInline value class OrderId(val id: String)

data class Order(val id: OrderId)

data object Empty

data object Mutable {
    var calls = 0
}

class Plain(val name: String)

sealed interface Outcome<out T>

data class Ok<out T>(val value: T) : Outcome<T>

data class Refused(val why: String) : Outcome<Nothing>

data class Maybe<T>(val content: T?)

data object Defaults {
    val note = Note("hi", null)
    val home = Empty
    val unit = ChronoUnit.DAYS
}

data object Registry {
    val items = mutableListOf<String>()
}

data object Workshop {
    val owner = Plain("Ana")
}
