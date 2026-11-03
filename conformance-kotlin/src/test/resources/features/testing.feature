@testing
Feature: Scenarios that use test support

  Scenario: The recording reporter keeps the path, the events and the global metadata
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
      | ship   |
    And step "charge" attempts:
      | attempt | outcome | code     |
      | 1       | failure | declined |
      | 2       | success |          |
    And step "charge" allows 1 retries
    And step "ship" sets the global metadata "user_id" to "u-7"
    And step "ship" succeeds
    And the workflow's ID generator returns "order-42"
    And the workflow lists the reporters "audit"
    And the executor uses its default dispatcher
    And the instance has the recording reporter of test support
    When the workflow runs
    Then the path is:
      | line                              |
      | charge attempt 1 failed: declined |
      | charge retrying                   |
      | charge attempt 2 succeeded        |
      | ship attempt 1 succeeded          |
      | journey succeeded                 |
    And the recording reporter of test support kept the same events as the reporter "audit"
    And the recording reporter of test support kept the global metadata:
      | key        | value      |
      | journey_id | "order-42" |
      | user_id    | "u-7"      |

  Scenario: A refused run leaves the path empty
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" succeeds
    And the executor is given a dispatcher factory that fails
    And the instance has the recording reporter of test support
    When the workflow runs
    Then run was refused
    And the path is empty

  Scenario: A fake replaces its provider and supplies its data
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" requests the service "payments" of type labeller
    And step "charge" allows 1 retries
    And step "charge" attempts:
      | attempt | outcome | code        |
      | 1       | failure | unreachable |
      | 2       | success |             |
    And the workflow declares the service provider "payments" for "payments" of type labeller, for each attempt
    And the test replaces the service provider "payments" with a fake that makes instances of second labeller and supplies the data {"mode": "test"}
    When the workflow runs
    Then the fake of "payments" was called 2 times
    And the service provider "payments" was called 0 times
    And step "charge" was given the service "payments" labelled "fake 2" for attempt 2
    And the events include, in order:
      | event            | step   | attempt | provider | implementation  | data             |
      | service_provided | charge | 1       | payments | second labeller | {"mode": "test"} |

  Scenario: A fake that fails aborts the journey
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" requests the service "payments" of type labeller
    And step "charge" succeeds
    And the workflow declares the service provider "payments" for "payments" of type labeller, once per journey
    And the test replaces the service provider "payments" with a fake that fails with "gateway down"
    When the workflow runs
    Then the fake of "payments" was called 1 times
    And the result's abort carries the error "gateway down"

  Scenario: Fakes that name no provider, or make another service, stop the test
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" requests the service "payments" of type labeller
    And step "charge" succeeds
    And the workflow declares the service provider "payments" for "payments" of type labeller, for each attempt
    And the test replaces the service provider "payments" with a fake that makes meters
    When the workflow runs
    Then the test stopped before the journey, naming:
      | mistake         | provider |
      | another service | payments |
    And no event was emitted

  Scenario: A step run alone contributes, emits and sets global metadata through the recorders
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" contributes "charged" = 40
    And step "charge" emits step_info "charging" with data {"amount": 40}
    And step "charge" sets the global metadata "journey_id" to "other"
    And step "charge" emits step_warning "after"
    And step "charge" succeeds
    When step "charge" runs alone
    Then the contributions recorder holds:
      | key     | value |
      | charged | 40    |
    And the step event reporter kept the events:
      | level | message  | data           |
      | info  | charging | {"amount": 40} |
    And the call step "charge" made setting "journey_id", and every call after it, was interrupted, and no call before it

  Scenario: A hook run alone with a failing recorder keeps nothing
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" succeeds
    And a step policy "audit" defines the hook "on step success"
    And the hook "on step success" of policy "audit" emits journey_warning "seen"
    And the hook "on step success" of policy "audit" contributes "seen" = true
    And step "charge" has the policies "audit"
    And the hook event reporter is failing
    When the hook "on step success" of policy "audit" runs alone
    Then every call the hook "on step success" of policy "audit" made to its hook reporter was interrupted
    And the hook event reporter kept nothing
    And the contributions recorder holds:
      | key  | value |
      | seen | true  |

  Scenario: An input adapter called alone reads a data bag built from values
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" requests input "amount" of type integer
    And step "charge" succeeds
    And the workflow declares the input adapter "amounts" for the steps "charge"
    And the input adapter "amounts" requests the data bag
    And the input adapter "amounts" returns "total" of type integer, read from the data bag, for "amount"
    When the input adapter "amounts" is called alone for the input "amount" of step "charge", with a data bag holding:
      | key   | value |
      | total | 40    |
    Then the input adapter returned 40

  Scenario: A fake that supplies no data replaces its provider
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" requests the service "payments" of type labeller
    And step "charge" succeeds
    And the workflow declares the service provider "payments" for "payments" of type labeller, once per journey
    And the test replaces the service provider "payments" with a fake that makes instances of first labeller
    When the workflow runs
    Then the fake of "payments" was called 1 times
    And the service provider "payments" was called 0 times
    And step "charge" was given the service "payments" labelled "fake 1" for attempt 1

  Scenario: A step run alone with a failing recorder keeps nothing
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" emits step_info "charging"
    And step "charge" sets the global metadata "user_id" to "u-7"
    And step "charge" succeeds
    And the step event reporter is failing
    When step "charge" runs alone
    Then every call step "charge" made to its step reporter was interrupted
    And the step event reporter kept nothing

  Scenario: A step run alone sets global metadata through the step event reporter
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" sets the global metadata "user_id" to "u-7"
    And step "charge" sets the global metadata "tenant" to "corner"
    And step "charge" succeeds
    When step "charge" runs alone
    Then the step event reporter kept the global metadata:
      | key     | value    |
      | user_id | "u-7"    |
      | tenant  | "corner" |

  Scenario: A hook run alone emits through the hook event reporter
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" succeeds
    And a step policy "audit" defines the hook "on step success"
    And the hook "on step success" of policy "audit" emits journey_warning "seen"
    And step "charge" has the policies "audit"
    When the hook "on step success" of policy "audit" runs alone
    Then the hook event reporter kept the events:
      | level   | message |
      | warning | seen    |
