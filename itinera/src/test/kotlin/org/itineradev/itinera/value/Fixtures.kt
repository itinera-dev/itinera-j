package org.itineradev.itinera.value

/** A parcel, as a record: a value, holding a list it does not copy. */
@JvmRecord data class Parcel(val weight: Long, val items: MutableList<String>)

/** A record whose copies fail once [Rebuilds.fail] is set, as a constructor with a check may. */
@JvmRecord
data class Ticket(val code: String) {
    init {
        check(!Rebuilds.fail) { "no more tickets" }
    }
}

/** Whether rebuilding a [Ticket] fails, set by the tests that need it. */
object Rebuilds {
    var fail = false
}

/** A record that cannot be hashed, so a set holding it cannot be copied. */
@JvmRecord
data class Fragile(val name: String) {
    @Suppress("ExceptionRaisedInUnexpectedLocation")
    override fun hashCode(): Int = throw IllegalStateException("cannot hash $name")

    override fun equals(other: Any?): Boolean = other is Fragile && other.name == name
}

/** An ordinary class, not a record: nothing says its fields are data. */
class Basket(val items: List<String>)

/** A Kotlin data class, which the core reads only with itinera-kotlin. */
data class Order(val amount: Long)

enum class Size {
    SMALL,
    LARGE,
}

/** An enum whose constants hold only immutable values, so they are shared. */
enum class Coin(val cents: Long) {
    PENNY(1L)
}

/** An enum whose constant has a body of its own. */
enum class Tone {
    LOUD {
        override fun toString(): String = "loud"
    }
}

/** An enum whose constants hold a field that can change. */
enum class Dial {
    LOW;

    var level = 0
}

/** An enum whose constants hold a list that can change. */
enum class Shelf(val items: MutableList<String>) {
    TOP(mutableListOf())
}
