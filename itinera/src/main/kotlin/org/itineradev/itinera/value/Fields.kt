package org.itineradev.itinera.value

import java.lang.reflect.Field
import java.lang.reflect.Modifier

/** The fields a class declares for each of its instances, leaving out its static ones. */
internal fun instanceFieldsOf(type: Class<*>): Sequence<Field> =
    type.declaredFields.asSequence().filterNot(::isStatic)

private fun isStatic(field: Field): Boolean = Modifier.isStatic(field.modifiers)
