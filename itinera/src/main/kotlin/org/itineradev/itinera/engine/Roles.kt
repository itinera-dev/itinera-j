package org.itineradev.itinera.engine

import org.itineradev.itinera.policy.Role

/** Whether [type] is a role: an interface marked `@Role`. */
internal fun isRole(type: Class<*>): Boolean =
    type.isInterface && type.isAnnotationPresent(Role::class.java)
