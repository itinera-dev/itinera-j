@defining
Feature: Scenarios that define their building blocks before the workflow and bind them after

  Scenario: Every table that defines policies, their hooks and what the hooks request
    Given the step policies and their hooks:
      | policy | hook            |
      | audit  | on step success |
      | audit  | on step failure |
    Given the workflow policies and their hooks:
      | policy | hook                |
      | guard  | on workflow success |
    Given the hooks request:
      | policy | hook            | request        | key  | type    | optional |
      | audit  | on step success | step name      |      |         |          |
      | audit  | on step success | attempt number |      |         |          |
      | audit  | on step success | journey ID     |      |         |          |
      | audit  | on step success | step data      | paid | integer | yes      |
    Given a workflow "orders" with the policies "guard" and the steps:
      | step   | policies | inputs          | outcome |
      | charge | audit    | amount: integer | success |
    And the workflow's ID generator returns "order-42"
    And the data bag contains:
      | key    | value |
      | amount | 10    |
    When the workflow runs
    Then the journey succeeded
    And the hooks received:
      | policy | hook                | calls | attempt number | step name | journey ID |
      | audit  | on step success     | 1     | 1              | charge    | order-42   |
      | audit  | on step failure     | 0     |                |           |            |
      | guard  | on workflow success | 1     |                |           |            |
    And the hook "on step success" of policy "audit" received step data "paid" absent

  Scenario: A hook that fails the workflow, and the steps that ended
    Given the step policies and their hooks:
      | policy | hook            | returns      | code    |
      | guard  | on step failure | FailWorkflow | refused |
    Given the hooks request:
      | policy | hook            | request       |
      | guard  | on step failure | failure cause |
    Given a workflow "orders" with the steps:
      | step   | policies | outcome | code     |
      | charge | guard    | failure | declined |
      | ship   |          | success |          |
    When the workflow runs
    Then the result names the step "charge" whose hook failed the journey, with the code "refused"
    And the hooks received:
      | policy | hook            | calls | cause   |
      | guard  | on step failure | 1     | failure |
    And the steps ended as:
      | step   | status       | attempts |
      | charge | failed       | 1        |
      | ship   | not executed | 0        |

  Scenario: Input adapters and service providers defined before the workflow and bound after
    Given the input adapter "pricing" returns 40 when step "charge" requests "amount"
    And the input adapter "pricing" returns nothing when step "ship" requests "amount"
    Given the service provider "labels" makes a labeller for "label", for each attempt
    Given the service provider "stock" makes a labeller for "stock", once per journey
    Given a workflow "orders" with the steps:
      | step   | inputs          | services        | outcome | rescue |
      | charge | amount: integer | label: labeller | success |        |
      | ship   | amount: integer | stock: labeller | success | yes    |
    And the workflow binds the input adapter "pricing" to the steps "charge", "ship"
    And step "ship" requests optional input "note" of type string
    And the workflow binds the service provider "labels" to the step "charge"
    And the workflow binds the service provider "stock" to the steps "ship"
    And the workflow lists the reporters "audit"
    And the data bag contains:
      | key    | value |
      | amount | 7     |
    When the workflow runs
    Then the journey succeeded
    And the steps were built with:
      | step   | attempt | input  | value |
      | charge |         | amount | 40    |
      | ship   | 1       | amount | 7     |
      | ship   |         | note   | absent |
    And the steps were given the services:
      | step   | service | label    | attempt |
      | charge | label   | labels 1 | 1       |
      | ship   | stock   | stock 1  | 1       |
    And the reporters received:
      | reporter | event             | received |
      | audit    | journey_started   | yes      |
      | audit    | journey_aborted   | no       |
      | audit    | journey_succeeded | 1        |

  Scenario: An input adapter that fails for one step, and a provider bound to no step
    Given the input adapter "ledger" requests the data bag
    And the input adapter "ledger" fails with "ledger down" when step "charge" requests "amount"
    Given the service provider "stock" makes a labeller for "stock", once per journey
    Given a workflow "orders" with the steps:
      | step   | inputs          | outcome |
      | charge | amount: integer | success |
    And the workflow binds the input adapter "ledger" to the step "charge"
    And the workflow binds the service provider "stock"
    When the workflow runs
    Then the journey was aborted with the abort reason "step could not be built"
    And the steps ended as:
      | step   | status  |
      | charge | aborted |
