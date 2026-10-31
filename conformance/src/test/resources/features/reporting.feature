@reporting
Feature: Scenarios whose steps emit events and set global metadata

  Scenario: A step's events and entries come in the order the step makes them
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" emits step_info "charging"
    And step "charge" sets the global metadata "tenant" to "acme"
    And step "charge" emits step_error "declined" with data {"code": "51"}
    And step "charge" succeeds
    And the workflow's ID generator returns "order-42"
    And the workflow lists the reporters "audit"
    And the executor uses its default dispatcher
    When the workflow runs
    Then the journey succeeded
    And the reporter "audit" was made with the journey ID "order-42"
    And the events include, in order:
      | event               | step   | attempt | message  | key    | source | data          |
      | step_info           | charge | 1       | charging |        |        |               |
      | global_metadata_set | charge | 1       |          | tenant | charge |               |
      | step_error          | charge | 1       | declined |        |        | {"code": "51"} |
      | step_succeeded      | charge | 1       |          |        |        |               |
    And the reporter "audit" received the global metadata:
      | key        | value      | after           |
      | journey_id | "order-42" | none            |
      | tenant     | "acme"     | step_info       |
    And no engine event carries the global metadata value of "tenant"

  Scenario: An entry under the journey ID's key aborts the journey
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" sets the global metadata "journey_id" to "forged"
    When the workflow runs
    Then the journey was aborted with the abort reason "reserved metadata key"
    And step "charge" ended as aborted

  Scenario: A step that ignores a reporter's failure is still aborted
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the workflow lists the reporters "audit"
    And the reporter "audit" throws on "step_warning"
    And step "charge" ignores failures of its emit calls and carries on
    And step "charge" emits step_warning "slow"
    And step "charge" sets the global metadata "tenant" to "acme"
    And step "charge" contributes "receipt" = "R-1"
    When the workflow runs
    Then the journey was aborted with the abort reason "reporter failed"
    And no contribution of "receipt" was committed
