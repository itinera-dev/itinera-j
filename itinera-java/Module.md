# Module itinera-java

Itinera for Java: the core, and what makes its API read naturally from Java, for synchronous workflows. A Java application adds this artifact, which brings the core, `itinera`, and nothing but Kotlin's standard library with it.

Most of what a Java application uses is the core's: workflows, steps, policies, roles, services, input adapters, the instances, `LocalExecutor` and the result of a journey, documented in the core's reference, which says how Java reads it. This artifact adds what only Java writes: requests whose answer is an `Optional`, and a parameter of an annotated Java class that requests one, which without it requests nothing a value can be. This reference shows its API in Java, and its examples in Kotlin, the only language Dokka's examples are written in.

Java writes synchronous workflows. Asynchronous workflows are written in Kotlin, with `itinera-kotlin`.

# Package org.itineradev.itinera.request.java

`JavaRequests`, which makes requests in Java's own forms, beside those `Requests` makes, such as an optional input whose answer is an `Optional`.
