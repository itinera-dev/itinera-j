import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Internal
import org.gradle.process.CommandLineArgumentProvider

/**
 * Lets javac read every jar of a class path as a module, those that are no Java module included,
 * each as an automatic module named after its file.
 */
abstract class ModulePath : CommandLineArgumentProvider {
    // Already an input of the compilation, as its class path.
    @get:Internal abstract val jars: ConfigurableFileCollection

    override fun asArguments(): List<String> = listOf("--module-path", jars.asPath)
}
