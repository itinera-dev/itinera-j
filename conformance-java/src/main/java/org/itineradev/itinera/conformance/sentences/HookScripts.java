package org.itineradev.itinera.conformance.sentences;

import org.itineradev.itinera.conformance.World;
import org.itineradev.itinera.conformance.model.Hook;
import org.itineradev.itinera.conformance.model.HookAction;
import org.itineradev.itinera.conformance.model.HookRequest;
import org.itineradev.itinera.conformance.model.HookReturn;
import org.itineradev.itinera.conformance.model.HookScript;

/** What the sentences add to the script of a hook. */
final class HookScripts {
  private HookScripts() {}

  /** Adds {@code request} to what {@code hook} of {@code policy} requests, after the others. */
  static void requests(World world, String hook, String policy, HookRequest request) {
    scriptOf(world, hook, policy).requests().add(request);
  }

  /** Adds {@code action} to what {@code hook} of {@code policy} does, after the others. */
  static void does(World world, String hook, String policy, HookAction action) {
    scriptOf(world, hook, policy).actions().add(action);
  }

  /** Says how {@code hook} of {@code policy} ends. */
  static void ends(World world, String hook, String policy, HookReturn ending) {
    var named = Hook.named(hook);
    world.model().workflow().policy(policy).hook(named).returns(named, policy, ending);
  }

  private static HookScript scriptOf(World world, String hook, String policy) {
    return world.model().workflow().policy(policy).hook(Hook.named(hook));
  }
}
