package org.itineradev.itinera.workflow.checker

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

class NamesTest {
    @Test
    fun `every name the checker reads is the binary name of a class that its declarer has`() {
        val names =
            Names::class.java.declaredFields.filter(::isName).map(::valueOf) +
                Hook.entries.map(Hook::annotation) +
                copyClasses +
                sharedClasses +
                stringTypes
        assertAll(names.map(::loading))
    }
}

private fun isName(field: Field): Boolean =
    Modifier.isPublic(field.modifiers) && field.type == String::class.java

private fun valueOf(field: Field): String = field.get(null) as String

private fun loading(name: String): Executable = Executable { Class.forName(name) }
