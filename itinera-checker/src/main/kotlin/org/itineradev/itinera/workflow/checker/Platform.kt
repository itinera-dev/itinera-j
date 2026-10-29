package org.itineradev.itinera.workflow.checker

/**
 * Whether the JDK or Kotlin's standard library declares the enum [type], whose constants are shared
 * values: one in a package under `kotlin`, or one that the platform's class loader finds, which
 * loads only what the JDK running the compiler declares in its modules of the platform.
 */
internal fun isPlatform(type: ClassSymbol): Boolean =
    type.name.substringBeforeLast('.', "").startsWith("kotlin.") || isFoundByThePlatform(type.name)

private fun isFoundByThePlatform(name: String): Boolean = runCatching {
    platformLoader.loadClass(name)
}
    .isSuccess

private val platformLoader: ClassLoader = ClassLoader.getPlatformClassLoader()
