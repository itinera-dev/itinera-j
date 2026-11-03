package org.itineradev.itinera.conformance.model

/** Whether a policy is attached to steps or to the workflow. */
internal enum class PolicyKind {
    STEP,
    WORKFLOW,
}

/** How building a scripted policy goes. */
internal sealed interface PolicyBuild {
    /** It is built. */
    data object Built : PolicyBuild

    /** Building it fails, with [message] when the case writes one. */
    data class Fails(val message: String?) : PolicyBuild
}

/** What a scenario scripts for one policy: its kind, how it is built and each hook it defines. */
internal class PolicyModel(val name: String, val kind: PolicyKind) {
    /** The hooks it defines, in the order first written. */
    val hooks = linkedMapOf<Hook, HookScript>()

    /** How building it goes. */
    var build: PolicyBuild = PolicyBuild.Built
        private set

    /** Says that building it fails, once. */
    fun failsToBuild(message: String?) {
        if (build != PolicyBuild.Built) {
            throw ModelException("how the policy \"$name\" fails to build is stated twice")
        }
        build = PolicyBuild.Fails(message)
    }

    /** The script of [hook], which the policy must define. */
    fun hook(hook: Hook): HookScript =
        hooks[hook]
            ?: throw ModelException("the policy \"$name\" does not define the hook \"$hook\"")
}
