@outcomes
Feature: Scenarios whose steps fail, skip and contribute

  Scenario: A failure is retried while the budget lasts, and only the last attempt's contributions are committed
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the data bag contains:
      | key    | value |
      | amount | 1     |
    And step "charge" allows 2 retries
    And step "charge" contributes "amount" = 2
    And step "charge" attempts:
      | attempt | outcome | code        | message      | details         | contributes        |
      | 1       | failure | unreachable | no answer    | {"tries": 1}    | {"draft": "D-1"}   |
      | 2       | success |             |              |                 | {"receipt": "R-1"} |
    When the workflow runs
    Then step "charge" ended as succeeded after 2 attempts
    And the events include, in order:
      | event                  | step   | key     | attempt | code        | cause   | source |
      | step_failed            | charge |         | 1       | unreachable |         |        |
      | step_retrying          | charge |         | 1       |             | failure |        |
      | step_succeeded         | charge |         | 2       |             |         |        |
      | contribution_committed | charge | amount  | 2       |             |         | charge |
      | data_overwritten       | charge | amount  | 2       |             |         | charge |
      | contribution_committed | charge | receipt | 2       |             |         | charge |
    And no contribution of "draft" was committed
    And the result's data bag contains:
      | key     | value |
      | amount  | 2     |
      | receipt | "R-1" |
    And the journey succeeded

  Scenario: A failure past the budget gives the step up
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
      | ship   |
    And step "charge" allows 1 retries
    And step "charge" attempts:
      | attempt | outcome | code     |
      | 1       | failure | declined |
    When the workflow runs
    Then step "charge" ended as failed after 2 attempts
    And step "ship" ended as not executed after 0 attempts
    And the events include, in order:
      | event         | step   | attempt | cause             |
      | step_given_up | charge | 2       | retries exhausted |
    And the journey failed

  Scenario: A skipped step discards its contributions, and the journey goes on
    Given a workflow "orders" with the steps:
      | step     |
      | discount |
      | ship     |
    And step "discount" attempts:
      | attempt | outcome | code             | contributes      |
      | 1       | skipped | no discount here | {"discount": 10} |
    And step "ship" requests optional input "discount" of type integer
    When the workflow runs
    Then step "discount" ended as skipped after 1 attempts
    And the events include, in order:
      | event                   | step     | attempt | code             |
      | step_skipped            | discount | 1       | no discount here |
      | contributions_discarded | discount | 1       |                  |
    And step "ship" was built with input "discount" absent
    And the result's data bag has no key "discount"
    And the journey succeeded

  Scenario: A skip with no reason
    Given a workflow "orders" with the steps:
      | step     |
      | discount |
    And step "discount" attempts:
      | attempt | outcome |
      | 1       | skipped |
    When the workflow runs
    Then step "discount" ended as skipped after 1 attempts
    And the journey succeeded

  Scenario: A contribution that is not a value aborts the journey
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" contributes a non-value as "callback"
    When the workflow runs
    Then the journey was aborted with the abort reason "not a value"
    And no contribution of "callback" was committed

  Scenario: A failure whose details are not a value aborts the journey
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" fails with code "declined" and details that are not a value
    When the workflow runs
    Then the journey was aborted with the abort reason "not a value"
