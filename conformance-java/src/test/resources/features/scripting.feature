@scripting
Feature: Scenarios that run a workflow with a script

  Scenario: A script plays every kind of row, and the record holds each attempt
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
      | ship   |
    And step "charge" requests input "amount" of type integer
    And step "charge" contributes "real" = true
    And step "charge" succeeds
    And step "charge" allows 3 retries
    And step "ship" succeeds
    And the data bag contains:
      | key    | value |
      | amount | 40    |
    And the script says:
      | step   | attempt   | contributes  | emits        | emit message | sets global metadata | ending               | code     | message |
      | charge |           |              |              |              |                      | failure              | declined |         |
      | charge | 2         | {"tried": 2} | step_warning | slow         | {"user_id": "u-7"}   | abnormal termination |          | timeout |
      | charge | 3         |              |              |              |                      | skipped              | paid     |         |
      | charge | remaining |              |              |              |                      | real run             |          |         |
      | ship   | every     |              |              |              |                      | success              |          |         |
    When the workflow runs with the script
    Then step "charge" ended as skipped after 3 attempts
    And the events include, in order:
      | event               | step   | attempt | message | key     |
      | step_failed         | charge | 1       |         |         |
      | step_warning        | charge | 2       | slow    |         |
      | global_metadata_set | charge | 2       |         | user_id |
      | step_skipped        | charge | 3       |         |         |
    And the scripted run's record of step "charge" holds the attempts 1, 2, 3
    And attempt 2 of step "charge" in the scripted run's record read the input "amount" = 40
    And reading attempt 4 of step "charge" in the scripted run's record reads nothing
    And the scripted run's record of step "ship" holds the attempts 1
    And reading the scripted run's record of step "refund" stops the test as the mistake "unknown step"

  Scenario: A script that leaves a step out, when every step must be scripted, stops the test
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
      | ship   |
    And step "charge" succeeds
    And step "ship" succeeds
    And the script says:
      | step   | attempt | ending  |
      | charge | 1       | success |
    And the script ensures every step is scripted
    When the workflow runs with the script
    Then the test stopped before the journey, naming:
      | mistake       | step |
      | step left out | ship |
    And no event was emitted

  Scenario: A step that declares no handles is still checked for the attempts it can have
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" declares no contributor and no reporter
    And step "charge" succeeds
    And the script says:
      | step   | attempt | emits     | emit message | ending  |
      | charge | 0       | step_info | charging     | success |
    When the workflow runs with the script
    Then the test stopped before the journey, naming:
      | mistake         | step   | attempt |
      | no such attempt | charge | 0       |
    And no event was emitted
