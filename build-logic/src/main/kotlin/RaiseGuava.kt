import javax.inject.Inject
import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.DirectDependencyMetadata

/**
 * Raises every dependency on Guava to [version], wherever it is resolved, so that the formatters
 * Spotless resolves on their own also get a release without known advisories.
 */
@CacheableRule
abstract class RaiseGuava @Inject constructor(private val version: String) : ComponentMetadataRule {
    override fun execute(context: ComponentMetadataContext) {
        context.details.allVariants {
            withDependencies { filter(::isGuava).forEach { it.version { require(version) } } }
        }
    }

    private fun isGuava(dependency: DirectDependencyMetadata) =
        dependency.group == "com.google.guava" && dependency.name == "guava"
}
