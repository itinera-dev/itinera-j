@rescues
Feature: Scenarios whose steps are rescued, and whose workflow hooks return a value

  Scenario: A rescue step receives what the failed attempt left, and its step runs again
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the data bag contains:
      | key    | value |
      | client | "c-9" |
    And step "charge" has a rescue step
    And step "charge" attempts:
      | attempt | outcome | code    | message       | details      | contributes        |
      | 1       | failure | expired | token expired | {"since": 3} | {"account": "A-1"} |
      | 2       | success |         |               |              |                    |
    And step "charge_rescue" requests input "client" of type string
    And step "charge_rescue" requests rescued step data "account" of type string
    And step "charge_rescue" requests optional rescued step data "card" of type string
    And step "charge_rescue" requests the failure reason
    And step "charge_rescue" contributes "token" = "new"
    And step "charge_rescue" succeeds
    When the workflow runs
    Then the events include, in order:
      | event                  | step          | attempt | cause   | rescue        |
      | step_rescuing          | charge        | 1       | failure | charge_rescue |
      | attempt_started        | charge_rescue | 1       |         |               |
      | step_succeeded         | charge_rescue | 1       |         |               |
      | contribution_committed | charge_rescue | 1       |         |               |
      | step_rescued           | charge        | 1       |         | charge_rescue |
      | step_succeeded         | charge        | 2       |         |               |
    And step "charge_rescue" was built with input "client" = "c-9"
    And step "charge_rescue" was built with rescued step data "account" = "A-1"
    And step "charge_rescue" was built with rescued step data "card" absent
    And step "charge_rescue" received the failure reason with code "expired", message "token expired" and details {"since": 3}
    And step "charge" ended as succeeded after 2 attempts
    And step "charge_rescue" ended as succeeded after 1 attempts
    And the journey succeeded

  Scenario: A rescue step that fails leaves its step rescuing, and fails the journey
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" has a rescue step
    And step "charge" attempts:
      | attempt | outcome | code    |
      | 1       | failure | expired |
    And step "charge_rescue" attempts:
      | attempt | outcome | code       |
      | 1       | failure | no-refresh |
    When the workflow runs
    Then step "charge" ended as rescuing after 1 attempts
    And step "charge_rescue" ended as failed after 1 attempts
    And the result names the failed step "charge_rescue" with the code "no-refresh"

  Scenario: A rescue step receives the error that ended the attempt
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" has a rescue step
    And step "charge" attempts:
      | attempt | outcome | message      |
      | 1       | error   | unauthorised |
      | 2       | success |              |
    And step "charge_rescue" requests the error
    And step "charge_rescue" succeeds
    When the workflow runs
    Then step "charge_rescue" received the error with the message "unauthorised"
    And the journey succeeded

  Scenario: A rescue step that cannot be built aborts the journey, and its step with it
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" has a rescue step
    And step "charge" attempts:
      | attempt | outcome | code    |
      | 1       | failure | expired |
    And step "charge_rescue" cannot be built because its constructor fails with "no client"
    When the workflow runs
    Then step "charge_rescue" ended as aborted
    And step "charge" ended as aborted

  Scenario: What a workflow hook returns is never read
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" succeeds
    And a workflow policy "closing" defines the hook "on workflow success"
    And the workflow has the policies "closing"
    And the hook "on workflow success" of policy "closing" returns a value
    When the workflow runs
    Then the hook "on workflow success" of policy "closing" was called 1 times
    And the journey succeeded
