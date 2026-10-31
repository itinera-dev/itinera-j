package org.itineradev.itinera.conformance.model

/**
 * Everything a scenario declares before it acts, as plain data, filled by its Given sentences. A
 * later stage builds the workflow, its instance and the executor from it, through itinera's public
 * API. A sentence that does not fit what is already declared is an error in the case.
 */
internal class Model {
    private var declared: WorkflowModel? = null
    private var startsEmpty = false

    /** The initial data of the workflow instance, in the order written. */
    val initialData = linkedMapOf<String, Any?>()

    /**
     * The initial global metadata of the workflow instance, each entry set in the order written.
     */
    val initialGlobalMetadata = mutableListOf<Pair<String, Any?>>()

    /** How each scripted reporter fails, by name; one not here never fails. */
    val reporterFailures = linkedMapOf<String, ReporterFailure>()

    /** What the executor is given to dispatch events. */
    var dispatching: Dispatching = Dispatching.Unstated
        private set

    /** Whether the executor accepts only synchronous steps, hooks, reporters and providers. */
    var onlySynchronous = false
        private set

    /** The workflow the scenario declared. */
    val workflow: WorkflowModel
        get() = declared ?: throw ModelException("no workflow is declared yet")

    /** Declares the workflow, once, with its steps in declaration order. */
    fun declare(name: String, steps: List<String>) {
        if (declared != null) throw ModelException("a workflow is already declared")
        declared = WorkflowModel(name, steps)
    }

    /** The script of a step, or of a rescue step, of the workflow. */
    fun step(name: String): StepScript =
        workflow.scripts[name] ?: throw ModelException("the workflow has no step \"$name\"")

    /**
     * Gives the step a rescue step, scripted under the name `<step>_rescue`. When a step of the
     * workflow already has that name, the two share one script, as two steps of one name do.
     */
    fun rescue(name: String) {
        step(name).hasRescueStep = true
        workflow.scripts.getOrPut("${name}_rescue", ::StepScript).isRescueStep = true
    }

    /** Says that the journey starts with no initial data. */
    fun emptyDataBag() {
        if (initialData.isNotEmpty())
            throw ModelException("the data bag already holds initial data")
        startsEmpty = true
    }

    /** Adds initial data, each key once. */
    fun contains(key: String, value: Any?) {
        if (startsEmpty) throw ModelException("the data bag was said to be empty")
        if (key in initialData) throw ModelException("the data bag holds \"$key\" twice")
        initialData[key] = value
    }

    /** Sets an entry of the initial global metadata, after those set before. */
    fun globalMetadata(key: String, value: Any?) {
        initialGlobalMetadata += key to value
    }

    /** Lists the workflow's reporters, once. */
    fun listReporters(reporters: List<String>) {
        if (workflow.reporters != null) throw ModelException("the reporters are listed twice")
        workflow.reporters = reporters
    }

    /** Says where the instance's journey ID comes from, once. */
    fun generateIdsWith(generator: IdGenerator) {
        if (workflow.idGenerator != null) throw ModelException("the ID generator is stated twice")
        workflow.idGenerator = generator
    }

    /** Says how a scripted reporter fails, once. */
    fun fails(reporter: String, failure: ReporterFailure) {
        if (reporter in reporterFailures) {
            throw ModelException("how the reporter \"$reporter\" fails is stated twice")
        }
        reporterFailures[reporter] = failure
    }

    /** Says what the executor is given to dispatch events, once. */
    fun dispatchWith(dispatching: Dispatching) {
        if (this.dispatching != Dispatching.Unstated) {
            throw ModelException("what the executor dispatches with is stated twice")
        }
        this.dispatching = dispatching
    }

    /** Says the executor accepts only synchronous parts. */
    fun acceptOnlySynchronous() {
        onlySynchronous = true
    }
}
