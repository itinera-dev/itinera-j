package org.itineradev.itinera.conformance.run;

import org.itineradev.itinera.policy.HookReporter;

/** A hook reporter each of whose calls is recorded and carried on past. */
final class CarriedOnHookReporter implements HookReporter {
  private final HookReporter lent;
  private final CarryingOn calls;

  /** {@code lent}, each of whose calls is recorded on {@code calls}. */
  CarriedOnHookReporter(HookReporter lent, CarryingOn calls) {
    this.lent = lent;
    this.calls = calls;
  }

  @Override
  public void info(String message) {
    calls.carriedOn("info", message, () -> lent.info(message));
  }

  @Override
  public void info(String message, Object data) {
    calls.carriedOn("info", message, () -> lent.info(message, data));
  }

  @Override
  public void warning(String message) {
    calls.carriedOn("warning", message, () -> lent.warning(message));
  }

  @Override
  public void warning(String message, Object data) {
    calls.carriedOn("warning", message, () -> lent.warning(message, data));
  }

  @Override
  public void error(String message) {
    calls.carriedOn("error", message, () -> lent.error(message));
  }

  @Override
  public void error(String message, Object data) {
    calls.carriedOn("error", message, () -> lent.error(message, data));
  }

  @Override
  public void setGlobalMetadata(String key, Object value) {
    calls.carriedOn(Call.SETTING, key, () -> lent.setGlobalMetadata(key, value));
  }
}
