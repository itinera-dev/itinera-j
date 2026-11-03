@journeys
Feature: Scenarios that run journeys and check what they did

  Scenario: Steps that succeed
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
      | ship   |
    And step "charge" succeeds
    And the data bag contains:
      | key    | value |
      | amount | 42    |
    And the workflow's ID generator returns "order-1"
    When the workflow runs
    Then the journey succeeded
    And run returned a result instead of throwing
    And the journey ID is "order-1"
    And the result's journey ID is "order-1"
    And the events are exactly:
      | event             | step   | attempt | decided by |
      | journey_started   |        |         |            |
      | attempt_started   | charge | 1       |            |
      | step_succeeded    | charge | 1       |            |
      | attempt_started   | ship   | 1       |            |
      | step_succeeded    | ship   | 1       |            |
      | journey_succeeded |        |         | default    |
    And the events include, in order:
      | event             | step |
      | step_succeeded    | ship |
      | journey_succeeded |      |
    And the last event is "journey_succeeded"
    And step "charge" ended as succeeded after 1 attempts
    And step "ship" was built 1 times, each time as a new instance
    And the result's data bag contains:
      | key    | value |
      | amount | 42    |
    And the result's data bag has no key "coupon"
    And the journey_started event lists the initial keys "amount" without their values
    And every event carries the journey ID "order-1" and the workflow name "orders", with increasing sequence numbers
    And no event carries the message "42"

  Scenario: Abnormal terminations that exhaust the retries
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
      | ship   |
    And step "charge" attempts:
      | attempt | outcome | message      |
      | 1       | error   | timeout      |
      | 2       | error   | gateway down |
    And step "charge" allows 1 retries
    When the workflow runs
    Then the journey failed
    And the result's failure has the cause "retries exhausted"
    And the result's failure carries the error "gateway down"
    And the result's failure carries no reason
    And step "charge" ended as failed after 2 attempts
    And step "charge" was built 2 times, each time as a new instance
    And step "ship" ended as not executed after 0 attempts
    And the events include, in order:
      | event                     | step   | attempt | cause                | decided by |
      | step_abnormal_termination | charge | 1       |                      |            |
      | step_retrying             | charge | 1       | abnormal termination | default    |
      | step_given_up             | charge | 2       | retries exhausted    | default    |
      | journey_failed            | charge |         | retries exhausted    |            |
    And the events include, in order:
      | event          | error        |
      | journey_failed | gateway down |

  Scenario: An abnormal termination with retries off
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" attempts:
      | attempt | outcome | message |
      | 1       | error   | timeout |
    And step "charge" allows 2 retries
    And step "charge" does not retry abnormal terminations
    When the workflow runs
    Then the journey failed
    And the result's failure has the cause "abnormal termination"
    And step "charge" ended as failed after 1 attempts

  Scenario: A step that cannot be built
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" cannot be built because its constructor fails with "no gateway"
    When the workflow runs
    Then the journey was aborted
    And the journey was aborted with the abort reason "step could not be built"
    And the result names the step "charge" as where the journey was aborted, with the abort reason "step could not be built"
    And the result's abort carries the error "no gateway"
    And the result carries no data bag
    And step "charge" ended as aborted
    And step "charge" ended as aborted after 1 attempts
    And no step ran
    And the last event is "journey_aborted"

  Scenario: A listed reporter that fails
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the workflow lists the reporters "audit", "metrics"
    And the executor uses its default dispatcher
    And the reporter "audit" fails with "disk full" on "attempt_started"
    When the workflow runs
    Then the journey was aborted with the abort reason "reporter failed"
    And the result's abort carries the error "disk full"
    And the reporter "audit" received the event "journey_started"
    And the reporter "audit" did not receive the event "step_succeeded"
    And the reporter "metrics" received the event "journey_aborted" 1 times
    And the last event is "journey_aborted"

  Scenario: Listed reporters and global metadata
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the workflow lists the reporters "audit", "metrics"
    And the executor uses its default dispatcher
    And the workflow's ID generator returns "order-7"
    And the workflow instance is created with the global metadata:
      | key    | value  |
      | tenant | "acme" |
    When the workflow runs
    Then the reporters "audit", "metrics" received the same events
    And the reporters "audit", "metrics" received the same global metadata
    And the reporter "audit" received the global metadata:
      | key        | value     | after |
      | journey_id | "order-7" | none  |
      | tenant     | "acme"    | none  |

  Scenario: A dispatcher factory that fails
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the executor is given a dispatcher factory that fails
    When the workflow runs
    Then run was refused
    And run was refused with the message "the dispatcher factory failed"
    And no event was emitted

  Scenario: A dispatcher that ignores the reporters the workflow lists
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the workflow lists the reporters "audit"
    And the executor is given a dispatcher holding the reporter "held" that ignores added reporters
    When the workflow runs
    Then the reporter "audit" received no event
    And the reporter "held" received the event "journey_succeeded"
    And the last event is "journey_succeeded"

  Scenario: One executor running two journeys
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the workflow has no ID generator
    And the workflow lists the reporters "audit"
    When the same executor runs the workflow twice, with a new instance each time
    Then the two journeys have different journey IDs
    And the journey ID is a UUID v4
    And the second journey's events are the first journey's events, apart from the journey ID and timestamps
    And each instance's reporter "audit" received only its own journey's events

  Scenario: A workflow the builder refuses
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
      | charge |
    When the workflow runs
    Then admission is refused with the violations:
      | violation           |
      | duplicate step name |
    And no journey ID was produced

  Scenario: A workflow only admitted
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    When the workflow is admitted
    Then no step ran

  Scenario: An ID generator that returns an empty string
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the workflow's ID generator returns ""
    When the workflow runs
    Then creating the workflow instance failed
    And no journey ID was produced
