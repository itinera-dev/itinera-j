package org.itineradev.itinera.engine

import java.lang.reflect.Method
import java.lang.reflect.Proxy
import org.itineradev.itinera.event.HookFailed
import org.itineradev.itinera.journey.Abort
import org.itineradev.itinera.policy.Role
import org.itineradev.itinera.step.StepName

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

/**
 * An object implementing only the role [type], which forwards every call to [workflow], so a hook
 * acting on [step], if any, reaches the workflow through the role alone and cannot cast its way to
 * the rest of it. An operation that fails aborts the journey at once with `hook failed`, carrying
 * what the operation threw, even when the hook catches it.
 */
internal fun Journey.roleOf(type: Class<*>, workflow: Any?, step: StepName?): Any =
    Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, arguments ->
        forwarded(method, workflow, arguments.orEmpty(), step)
    }

private fun Journey.forwarded(
    method: Method,
    workflow: Any?,
    arguments: Array<out Any?>,
    step: StepName?,
): Any? {
    method.trySetAccessible()
    return runCatching { invoked(method, workflow, *arguments) }
        .onFailure(::rethrowError)
        .getOrElse { thrown -> throw abort(Abort(HookFailed(step, messageOf(thrown)), thrown)) }
}
