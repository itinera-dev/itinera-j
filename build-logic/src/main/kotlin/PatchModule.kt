import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider

/**
 * Lets javac compile a module descriptor beside classes that another compiler made, by patching
 * those classes into the module.
 */
abstract class PatchModule : CommandLineArgumentProvider {
    @get:Input abstract val module: Property<String>

    // Already an input of the compilation, through its class path.
    @get:Internal abstract val classes: DirectoryProperty

    override fun asArguments(): List<String> =
        listOf("--patch-module", "${module.get()}=${classes.get().asFile}")
}
