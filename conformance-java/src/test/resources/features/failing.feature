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

  @unmet
  Scenario: A journey that did not do what the case expects
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    When the workflow runs
    Then the journey failed

  @not-run-yet
  Scenario: A step whose attempt fails, which the runner cannot run yet
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" attempts:
      | attempt | outcome | code     |
      | 1       | failure | declined |
    When the workflow runs
    Then the journey failed
