package org.itineradev.itinera.value.kotlin

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaField
import org.itineradev.itinera.engine.AnyValueType
import org.itineradev.itinera.engine.Component
import org.itineradev.itinera.engine.DataClass
import org.itineradev.itinera.engine.DataObject
import org.itineradev.itinera.engine.KotlinClasses
import org.itineradev.itinera.engine.KotlinFunction
import org.itineradev.itinera.engine.KotlinShape
import org.itineradev.itinera.engine.NotData
import org.itineradev.itinera.engine.rethrowError
import org.itineradev.itinera.workflow.kotlin.kotlinFunctionOf

/**
 * Reads Kotlin classes for the core, which finds it through `java.util.ServiceLoader`: a data class
 * whose only fields, its superclasses' included, are the `val` properties of its primary
 * constructor, none of a value class, and a `data object` whose fields and properties are all
 * final; and what a method of an annotated class declares.
 */
internal class KotlinDataClasses : KotlinClasses {
    override fun shapeOf(type: Class<*>): KotlinShape =
        if (isClass(type))
            runCatching { shapeOf(type.kotlin) }.onFailure(::rethrowError).getOrDefault(NotData)
        else NotData

    private fun shapeOf(type: KClass<*>): KotlinShape =
        when {
            !type.isData -> NotData
            type.objectInstance != null -> dataObject(type)
            else -> dataClass(type)
        }

    override fun functionOf(method: Method): KotlinFunction = kotlinFunctionOf(method)
}

/**
 * Whether Kotlin compiled the class as a class, rather than as a lambda, a file's functions or
 * another synthetic class, which kotlin-reflect cannot read.
 */
private fun isClass(type: Class<*>): Boolean =
    type.getAnnotation(Metadata::class.java)?.kind == CLASS

private const val CLASS = 1

/**
 * An object's own properties are held in static fields, so they are read besides its instance
 * fields.
 */
private fun dataObject(type: KClass<*>): KotlinShape {
    val fields =
        instanceFields(type.java) +
            type.declaredMemberProperties.mapNotNull(KProperty1<*, *>::javaField)
    return if (fields.all(::isFinal)) DataObject(fields.map(::untypedComponent)) else NotData
}

private fun dataClass(type: KClass<*>): KotlinShape {
    val constructor = type.primaryConstructor ?: return NotData
    val properties = type.declaredMemberProperties.associateBy { property -> property.name }
    val parameters = constructor.parameters
    val components = parameters.mapNotNull { parameter -> component(parameter, properties) }
    val javaConstructor = constructor.javaConstructor
    val fields = components.map(Component::field).toSet()
    return if (
        javaConstructor == null ||
            components.size != parameters.size ||
            fields != instanceFields(type.java).toSet()
    )
        NotData
    else DataClass(components, javaConstructor)
}

private fun component(
    parameter: KParameter,
    properties: Map<String, KProperty1<*, *>>,
): Component? {
    val property = properties[parameter.name] ?: return null
    val field = property.javaField
    val ofValueClass = (parameter.type.classifier as? KClass<*>)?.isValue == true
    return if (field == null || property is KMutableProperty<*> || ofValueClass) null
    else Component(field, valueType(parameter.type))
}

private fun instanceFields(type: Class<*>): List<Field> =
    generateSequence(type, Class<*>::getSuperclass)
        .flatMap(::fieldsOf)
        .filterNot(::isStatic)
        .toList()

private fun fieldsOf(type: Class<*>): Sequence<Field> = type.declaredFields.asSequence()

private fun isStatic(field: Field): Boolean = Modifier.isStatic(field.modifiers)

private fun isFinal(field: Field): Boolean = Modifier.isFinal(field.modifiers)

private fun untypedComponent(field: Field): Component = Component(field, AnyValueType)
