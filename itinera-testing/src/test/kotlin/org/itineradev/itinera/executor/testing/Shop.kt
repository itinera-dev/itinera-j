package org.itineradev.itinera.executor.testing

import org.itineradev.itinera.instance.WorkflowInstance
import org.itineradev.itinera.instance.instance
import org.itineradev.itinera.report.Reporter
import org.itineradev.itinera.report.testing.RecordingReporter
import org.itineradev.itinera.request.Requests
import org.itineradev.itinera.step.Outcome
import org.itineradev.itinera.step.RescueOutcome
import org.itineradev.itinera.step.SyncRescueRun
import org.itineradev.itinera.step.SyncStepFactory
import org.itineradev.itinera.step.SyncStepRun
import org.itineradev.itinera.value.TypeToken
import org.itineradev.itinera.workflow.RescueStepDescriptor
import org.itineradev.itinera.workflow.StepDescriptor
import org.itineradev.itinera.workflow.Sync
import org.itineradev.itinera.workflow.WorkflowDescriptor

/** The shop whose orders the workflows take. */
class Shop

val LONG: TypeToken<Long> = TypeToken.of(Long::class.javaObjectType)

private val CONTRIBUTOR = Requests.contributor()
private val REPORTER = Requests.stepReporter()
private val CHARGED = Requests.input("charged", LONG)
private val TRIED = Requests.optionalFromStep("tried", LONG)
private val SUCCEEDING = SyncStepFactory { SyncStepRun { Outcome.success() } }

/**
 * Takes orders, each step written to be scripted:
 * 1. `charge`, retried once, declares a contributor and a step reporter, and for real contributes
 *    40 under `charged` and succeeds;
 * 2. `pack` declares nothing and succeeds;
 * 3. `label`, retried twice but never after an abnormal termination, succeeds;
 * 4. `fix` declares a contributor and succeeds, and its rescue step, `fix_rescue`, requests what it
 *    contributed under `tried` and declares a contributor;
 * 5. `ship` requires `charged` and succeeds.
 */
val orders: WorkflowDescriptor<Shop, Sync> =
    WorkflowDescriptor.builder(Shop::class.java, "orders")
        .step(
            StepDescriptor.builder("charge") { got ->
                    SyncStepRun {
                        got[CONTRIBUTOR].contribute("charged", 40L)
                        Outcome.success()
                    }
                }
                .request(CONTRIBUTOR)
                .request(REPORTER)
                .retryBudget(1)
                .build()
        )
        .step(StepDescriptor.builder("pack", SUCCEEDING).build())
        .step(
            StepDescriptor.builder("label", SUCCEEDING)
                .retryBudget(2)
                .noRetryOnAbnormalTermination()
                .build()
        )
        .step(
            StepDescriptor.builder("fix", SUCCEEDING)
                .request(CONTRIBUTOR)
                .rescue(
                    RescueStepDescriptor.builder { SyncRescueRun { RescueOutcome.success() } }
                        .request(TRIED)
                        .request(CONTRIBUTOR)
                        .build()
                )
                .build()
        )
        .step(StepDescriptor.builder("ship", SUCCEEDING).request(CHARGED).build())
        .build()

/** An instance of [orders], reporting to [reporter]. */
fun ordered(reporter: Reporter = RecordingReporter()): WorkflowInstance<Sync> =
    orders.instance(Shop()).reporter(reporter).create()
