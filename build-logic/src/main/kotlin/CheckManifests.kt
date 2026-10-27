import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails unless the Kotlin and the Java conformance manifests agree, as [ManifestAgreement] says.
 */
abstract class CheckManifests : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val kotlinManifest: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val javaManifest: RegularFileProperty

    @TaskAction
    fun check() {
        val problems = ManifestAgreement.problems(read(kotlinManifest), read(javaManifest))
        if (problems.isNotEmpty())
            throw GradleException(problems.joinToString("\n", "The manifests disagree:\n"))
    }

    private fun read(manifest: RegularFileProperty): Map<*, *> =
        JsonSlurper().parse(manifest.get().asFile) as? Map<*, *>
            ?: throw GradleException("${manifest.get().asFile.name} must hold an object.")
}
