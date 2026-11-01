@declaring
Feature: Scenarios that only declare

  Scenario: A workflow of one step that succeeds
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" succeeds

  @capability-sync
  Scenario: A scenario that needs synchronous steps
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the executor accepts only synchronous steps, hooks and reporters

  @capability-async
  Scenario: A scenario that needs asynchronous steps
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" is asynchronous
    And the workflow declares the input adapter "pricing" for the steps "charge"
    And the input adapter "pricing" is asynchronous

  Scenario: Every sentence that declares a workflow
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
      | ship   |
    And step "charge" requests input "amount" of type integer
    And step "charge" requests optional input "coupon" of type string
    And step "charge" contributes "receipt" = {"id": 7}
    And step "charge" attempts:
      | attempt | outcome | code     | message                | details        | contributes   |
      | 1       | failure | declined | The card was declined. | {"amount": 10} | {"tries": 1}  |
      | 2       | success |          |                        |                |               |
    And step "charge" does not retry abnormal terminations
    And step "charge" allows 2 retries
    And step "charge" has a rescue step
    And step "ship" succeeds

  Scenario: Every sentence that declares the instance
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the data bag contains:
      | key    | value |
      | amount | 10    |
    And the workflow's ID generator returns "order-42"
    And the workflow instance is created with the global metadata:
      | key    | value  |
      | tenant | "acme" |

  Scenario: A workflow with no ID generator and no initial data
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the data bag is empty
    And the workflow has no ID generator

  Scenario: Every sentence that declares reporters
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the workflow lists the reporters "audit", "metrics"
    And the executor is given a dispatcher holding the reporter "audit"
    And the reporter "audit" throws
    And the reporter "metrics" fails when it receives the global metadata "tenant"

  Scenario: A dispatcher that ignores added reporters
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the executor is given a dispatcher holding the reporter "audit" that ignores added reporters
    And the reporter "audit" throws on "journey_started"

  Scenario: A dispatcher that throws when a reporter is added
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the executor is given a dispatcher holding the reporter "audit" that throws when a reporter is added
    And the reporter "audit" fails with "full" on "journey_started"

  Scenario: A dispatcher that throws on one kind of event
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the executor is given a dispatcher holding the reporter "audit" that throws when dispatching "step_succeeded"

  Scenario: The executor's own dispatchers
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the executor uses its default dispatcher

  Scenario: A dispatcher factory that fails
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the executor is given a dispatcher factory that fails
