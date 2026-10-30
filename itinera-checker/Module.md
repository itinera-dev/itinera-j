# Module itinera-checker

Itinera's compile-time checks: the mistakes of annotated workflow classes reported as compile errors, such as a hook declared to return what its kind does not, or a step whose service no provider serves, instead of being refused when the workflow is first described. Each error says what describing the workflow would refuse, in the same words.

An application adds it to its compiler's processor path, never to its own dependencies:

- in Java, `annotationProcessor("org.itinera-dev:itinera-checker")`, where javac finds `JavacChecker`, a compiler plugin, and starts it with no other option. It is no annotation processor, so it claims no annotation, hides none from the application's processors, and gives javac's `processing` lint nothing to warn of, so an application without other annotation processors may compile with `-Xlint:all -Werror`; when another processor runs, that lint still warns of each annotation no processor claims, itinera's among them, which `-Xlint:all,-processing` silences. It runs whenever javac compiles, even with `-proc:none`, but with `-proc:only` only when an annotation processor runs too, and javac also starts it from the class path when no processor path is given, so a build turns it off by taking it off both;
- in Kotlin, with KSP's Gradle plugin applied, `ksp("org.itinera-dev:itinera-checker")`, which KSP runs as `KspChecker`.

javac checks the workflows written in Java whose parts are all Java classes, and KSP every workflow that is written in Kotlin, extends a Kotlin class or names a part written in Kotlin, so no workflow is checked twice. What only the running application can tell, such as whether a module opens its packages to itinera, is still checked when the workflow is described.

# Package org.itineradev.itinera.workflow.checker

`JavacChecker`, the compiler plugin javac starts, and `KspChecker`, the symbol processor KSP runs.
