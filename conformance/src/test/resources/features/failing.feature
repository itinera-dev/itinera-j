Feature: Scenarios that fail before itinera runs

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
