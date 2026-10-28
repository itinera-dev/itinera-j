package org.itineradev.itinera.engine

import org.itineradev.itinera.policy.Role

/** Whether [type] is a role: an interface marked `@Role`. */
internal fun isRole(type: Class<*>): Boolean =
    type.isInterface && type.isAnnotationPresent(Role::class.java)

/** The roles [type] provides: every interface marked `@Role` that it implements. */
internal fun <W> rolesOf(type: Class<*>): List<Class<in W>> =
    interfacesOf(type).filter(::isRole).distinct().map(::returned)

private fun interfacesOf(type: Class<*>): List<Class<*>> =
    generateSequence(type, Class<*>::getSuperclass).toList().flatMap(::implemented)

private fun implemented(type: Class<*>): List<Class<*>> =
    type.interfaces.flatMap(::withSuperinterfaces)

private fun withSuperinterfaces(type: Class<*>): List<Class<*>> = listOf(type) + implemented(type)
