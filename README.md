# itinera-j

The JVM implementation of [Itinera](https://itinera-dev.org/), a workflow framework that keeps business rules separate from flow control, for Kotlin and for Java. It implements the [Itinera specification](https://github.com/itinera-dev/spec), whose vocabulary it uses.

A workflow is a list of steps. Each step holds one piece of business logic and says only how it ended: it succeeded, it failed with a reason, or it was skipped. Everything around the steps, such as retrying a failed attempt, passing data from one step to the next, lending a step the services it needs, and telling every reporter what happened, is the executor's work, done the same way for every workflow. Policies attached to a step or to the whole workflow change those decisions in one place, without touching the steps.

## Installing it

From the first release on, the artifacts are published to Maven Central under the group `org.itinera-dev`, all at one version, which the bill of materials, `itinera-bom`, names once. A Kotlin application adds `itinera-kotlin`, and a Java application `itinera-java`; both bring the core, `itinera`. Test support is `itinera-testing`, for an application's tests only. With Gradle:

```kotlin
dependencies {
    implementation(platform("org.itinera-dev:itinera-bom:VERSION"))
    implementation("org.itinera-dev:itinera-kotlin")
    testImplementation("org.itinera-dev:itinera-testing")
}
```

A Java application writes `itinera-java` in place of `itinera-kotlin`. With Maven, the bill of materials is imported under `dependencyManagement`, and each artifact is then added without a version, here for a Java application:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.itinera-dev</groupId>
      <artifactId>itinera-bom</artifactId>
      <version>VERSION</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>org.itinera-dev</groupId>
    <artifactId>itinera-java</artifactId>
  </dependency>
  <dependency>
    <groupId>org.itinera-dev</groupId>
    <artifactId>itinera-testing</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

The artifacts run on JDK 17 and later. Java applications write synchronous workflows, and Kotlin applications synchronous ones or asynchronous ones on coroutines. A Kotlin application needs `itinera-kotlin` for synchronous workflows too, since it is what reads Kotlin classes and data classes.

### Compile-time checks

A Java application can also have javac report the mistakes of its annotated workflows as compile errors, such as a hook declared to return what its kind does not, by adding `itinera-checker` to its annotation processors; without it, the same mistakes are refused when the workflow is first described:

```kotlin
dependencies {
    annotationProcessor(platform("org.itinera-dev:itinera-bom:VERSION"))
    annotationProcessor("org.itinera-dev:itinera-checker")
}
```

A Kotlin application has KSP report them instead, by applying KSP's Gradle plugin, at the release the checker is built against, which the example below names, and adding the checker to its symbol processors. KSP checks the workflows written in Kotlin, and those written in Java that extend a Kotlin class or name a part written in Kotlin; javac's checker checks the other Java workflows:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "2.3.12"
}

dependencies {
    ksp(platform("org.itinera-dev:itinera-bom:VERSION"))
    ksp("org.itinera-dev:itinera-checker")
}
```

## A first workflow

The workflow below charges an order and then ships it. A failed charge is retried up to twice, and a charge of nothing fails, so the second journey fails once its retries are exhausted. Each step is a class marked `@Step`, which requests its input by key, and the contributor through which it adds to the data bag; the workflow is a class marked `@Workflow`, which lists its steps in the order they run.

### In Kotlin

```kotlin
package com.example.orders

import org.itineradev.itinera.executor.LocalExecutor
import org.itineradev.itinera.journey.Contributor
import org.itineradev.itinera.journey.FailureCause
import org.itineradev.itinera.journey.kotlin.output
import org.itineradev.itinera.step.Input
import org.itineradev.itinera.step.ItineraStep
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.Step
import org.itineradev.itinera.workflow.ItineraWorkflow
import org.itineradev.itinera.workflow.WithStep
import org.itineradev.itinera.workflow.Workflow

@Step
class Charge(@Input private val amount: Long) : ItineraStep {
    fun run(contributor: Contributor): Outcome {
        if (amount <= 0) return fail("nothing to charge")
        contributor.contribute("receipt", "charged $amount")
        return succeed()
    }
}

@Step
class Ship(@Input private val receipt: String) : ItineraStep {
    fun run(contributor: Contributor): Outcome {
        contributor.contribute("shipped", "after $receipt")
        return succeed()
    }
}

@Workflow(name = "orders")
@WithStep(Charge::class, name = "charge", retryBudget = 2)
@WithStep(Ship::class, name = "ship")
class Orders : ItineraWorkflow

data class Shipment(val shipped: String)

fun main() {
    val executor = LocalExecutor()

    val paid = executor.run(Orders().instance().data("amount", 40L).create())
    check(paid.isSucceeded)
    check(paid.output<Shipment>() == Shipment("after charged 40"))

    val free = executor.run(Orders().instance().data("amount", 0L).create())
    check(free.isFailed)
    check(free.failure?.cause == FailureCause.RETRIES_EXHAUSTED)
    check(free.failure?.reason?.code == "nothing to charge")
}
```

### In Java

```java
package com.example.orders;

import org.itineradev.itinera.executor.LocalExecutor;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.journey.FailureCause;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.WithStep;
import org.itineradev.itinera.workflow.Workflow;

public final class FirstWorkflow {
  @Step
  public static final class Charge implements ItineraStep {
    private final long amount;

    public Charge(@Input("amount") long amount) {
      this.amount = amount;
    }

    public Outcome run(Contributor contributor) {
      if (amount <= 0) return fail("nothing to charge");
      contributor.contribute("receipt", "charged " + amount);
      return succeed();
    }
  }

  @Step
  public static final class Ship implements ItineraStep {
    private final String receipt;

    public Ship(@Input("receipt") String receipt) {
      this.receipt = receipt;
    }

    public Outcome run(Contributor contributor) {
      contributor.contribute("shipped", "after " + receipt);
      return succeed();
    }
  }

  @Workflow(name = "orders")
  @WithStep(step = Charge.class, name = "charge", retryBudget = 2)
  @WithStep(step = Ship.class, name = "ship")
  public static final class Orders implements ItineraWorkflow {}

  public record Shipment(String shipped) {}

  public static void main(String[] args) throws Refusal {
    var executor = new LocalExecutor();

    var paid = executor.run(new Orders().instance().data("amount", 40L).create());
    check(paid.isSucceeded());
    check(paid.output(TypeToken.of(Shipment.class)).equals(new Shipment("after charged 40")));

    var free = executor.run(new Orders().instance().data("amount", 0L).create());
    check(free.isFailed());
    check(free.failure().cause() == FailureCause.RETRIES_EXHAUSTED);
    check(free.failure().reason().code().equals("nothing to charge"));
  }

  private static void check(boolean holds) {
    if (!holds) throw new IllegalStateException("The orders workflow did not run as expected.");
  }
}
```

## The parts of a workflow

- A **step** holds one business rule. It requests the data it needs, which the workflow supplies, runs once per attempt, and reports an outcome: success, failure or skipped. Only an attempt that succeeds commits what the step contributed. A step may have a **rescue step**, which runs when the step fails or terminates abnormally and, once it succeeds, lets the step run again.
- A **workflow** lists its steps in the order they run, with their retry budgets and step policies, its workflow policies, its input adapters, its service providers, the roles it provides, its ID generator and its reporters. It is described once, before any journey, from its annotations or with the builder, and a description that breaks a rule of the specification is refused with `Violations`.
- A **step policy** holds flow control for the steps it is attached to. Its hooks, `on step success`, `on step failure`, `on step retry` and `on step abnormal termination`, are called in response to a step's outcome, and may end the journey early.
- A **workflow policy** acts when the journey ends: its hooks, `on workflow success` and `on workflow failure`, are called before the result is produced, and what they contribute is part of the journey's output. Any hook may use a role, an interface of operations the workflow provides, such as sending a notification.
- What happens in a journey is recorded as **events**, which the journey's reporters receive, together with the journey's **global metadata**: entries of a key and a value, apart from the data bag, starting with the journey ID.
- A **service** is an object of operations a step uses to do its business, such as a payment gateway. The step requests it by key and type, and a service provider of the workflow makes it, for the journey or for each attempt.
- A **workflow instance** is created for each journey, with its initial data, its reporters and any initial global metadata, and an **executor** runs it to a **result**: the journey succeeded, failed or was aborted.

## What each artifact holds

| Artifact | What it holds |
|---|---|
| `itinera` | The core, which the other artifacts bring: everything a synchronous workflow needs, written in Kotlin and used from Kotlin and Java alike |
| `itinera-kotlin` | Kotlin classes and data classes read as Kotlin declares them, asynchronous workflows on coroutines with `AsyncLocalExecutor`, and reified forms of the core's API |
| `itinera-java` | Requests in Java's own forms, such as an optional input whose answer is an `Optional` |
| `itinera-testing` | Test support, for an application's tests: mocks of steps, rescue steps and policies, recording reporters, recorders, fake providers and scripted runs |
| `itinera-checker` | Compile-time checks of annotated workflows, for javac and KSP |
| `itinera-bom` | The bill of materials, which names the version of every artifact once |

## Learning more

- [The API reference](https://javadoc.io/doc/org.itinera-dev/itinera), on javadoc.io for each release, documents every item of every artifact, each with an example. Its first page for the core says how Java reads it.
- [The Itinera site](https://itinera-dev.org/) explains why Itinera exists.
- [The specification](https://github.com/itinera-dev/spec) defines what every implementation of Itinera does.

## Status

No version has been released yet. The [changelog](https://github.com/itinera-dev/itinera-j/blob/main/CHANGELOG.md) will say what each release implements: the specification version, the tier, the capabilities each language claims and the conformance cases it passes, whose reports each [GitHub release](https://github.com/itinera-dev/itinera-j/releases) will carry.

## Rules made impossible to express

The specification lets an implementation make some rules impossible to express rather than check them while a workflow runs. In Kotlin and in Java, these mistakes do not compile. Where an annotated class can still declare one, such as a hook declared to return a lifecycle its kind may not return, the workflow is refused when it is described and, in an application that adds `itinera-checker`, the mistake is reported as a compile error:

- a hook returning a lifecycle it may not return;
- a part of the other mode in a workflow, such as an asynchronous step, input adapter or service provider in a synchronous workflow;
- a rescue step that skips;
- a service provider of journey scope requesting the attempt;
- an input adapter writing to the data bag through its access;
- a test's script choosing a fraction of an attempt.

In Kotlin, two more do not compile: custom code returning what its call cannot return, such as a step's run returning no outcome, and an ID generator returning something that is not a string. In Java, a method declared to return something else is refused when the workflow is described, and a `null` returned where an outcome or a journey ID is awaited is caught when it happens.

## Contributing

See the [contributor guide](https://github.com/itinera-dev/itinera-j/blob/main/docs/contributing.md) for how work arrives here, how to build and test, and the plan being implemented, and the [organisation's contributing guide](https://github.com/itinera-dev/.github/blob/main/CONTRIBUTING.md) for the rules every itinera-dev repository follows.

Built with AI under the terms of [A manifesto for software engineering with AI](https://marlon-sousa.com/blog/manifesto/); see [how Itinera is built](https://github.com/itinera-dev/.github/blob/main/CONTRIBUTING.md#how-itinera-is-built).

## Licence

Licensed under either of

- Apache License, Version 2.0 ([LICENSE-APACHE](https://github.com/itinera-dev/itinera-j/blob/main/LICENSE-APACHE) or <https://www.apache.org/licenses/LICENSE-2.0>)
- MIT license ([LICENSE-MIT](https://github.com/itinera-dev/itinera-j/blob/main/LICENSE-MIT) or <https://opensource.org/licenses/MIT>)

at your option.

### Contribution

Unless you explicitly state otherwise, any contribution intentionally submitted
for inclusion in the work by you, as defined in the Apache-2.0 license, shall be
dual licensed as above, without any additional terms or conditions.
