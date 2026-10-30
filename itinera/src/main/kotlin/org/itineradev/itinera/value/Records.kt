package org.itineradev.itinera.value

import java.lang.reflect.RecordComponent
import org.itineradev.itinera.engine.Component

/** A record, read through its fields and rebuilt through its canonical constructor. */
internal fun recordShape(type: Class<*>): Shape {
    val nullMarked = isNullMarked(type)
    val components =
        type.recordComponents.map { component -> recordComponent(type, component, nullMarked) }
    val parameters = type.recordComponents.map { component -> component.type }
    return composite(components, type.getDeclaredConstructor(*parameters.toTypedArray()))
}

private fun recordComponent(
    type: Class<*>,
    component: RecordComponent,
    nullMarked: Boolean,
): Component =
    Component(
        type.getDeclaredField(component.name),
        declaredType(component.annotatedType, nullMarked),
    )
