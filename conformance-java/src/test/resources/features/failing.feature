Feature: Scenarios that fail the run

  @undefined
  Scenario: A sentence the runner does not define
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    When a sentence no runner defines

  @wrong-case
  Scenario: A step the workflow does not have
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "refund" succeeds

  @unknown-column
  Scenario: A table with a column its sentence does not define
    Given a workflow "orders" with the steps:
      | step   | colour |
      | charge | blue   |

  @deferred-check
  Scenario: A hook that calls an operation its role does not have, defined before the workflow
    Given a step policy "tell" defines the hook "on step success"
    And the hook "on step success" of policy "tell" requests the role "notifier" and calls its operation "notify"
    Given a workflow "orders" with the steps:
      | step   | policies | outcome |
      | charge | tell     | success |
    And the workflow provides the role "notifier" with the operation "alert"
    When the workflow runs

  @unmet
  Scenario: A journey that did not do what the case expects
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    When the workflow runs
    Then the journey failed
