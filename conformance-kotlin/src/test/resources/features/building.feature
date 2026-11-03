@building
Feature: Scenarios that build steps from their inputs and services

  Scenario: Every sentence about inputs and input adapters
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
      | ship   |
    And the data bag contains:
      | key      | value           |
      | price    | 40              |
      | items    | ["book", "pen"] |
      | currency | "EUR"           |
    And step "charge" requests input "amount" of type integer
    And step "charge" requests optional input "note" of type string
    And step "charge" requests input "caller" of type string
    And step "charge" requests input "items" of type list
    And step "charge" requests input "tax" of type integer
    And step "charge" changes its input "items" to ["nothing"]
    And step "charge" succeeds
    And step "ship" requests input "items" of type list
    And step "ship" requests optional input "total" of type integer
    And step "ship" succeeds
    And the workflow declares the input adapter "pricing" for the steps "charge", "ship"
    And the input adapter "pricing" requests data from the workflow "currency" of type string
    And the input adapter "pricing" requests the data bag
    And the input adapter "pricing" returns "price" of type integer, read from the data bag, for "amount"
    And the input adapter "pricing" requests the step name and returns it for "caller"
    And the input adapter "pricing" returns nothing for "items"
    And the input adapter "pricing" returns 7 for "tax"
    And the input adapter "pricing" keeps its data bag access when called for step "charge", and returns "price" of type integer, read through that kept access, for "total" when called for step "ship"
    When the workflow runs
    Then the journey succeeded
    And step "charge" was built with input "amount" = 40
    And step "charge" was built with input "note" absent
    And step "charge" was built with input "caller" = "charge"
    And step "charge" was built with input "tax" = 7
    And step "ship" was built for attempt 1 with input "items" = ["book", "pen"]
    And step "ship" was built with input "total" absent
    And the events include, in order:
      | event                  | step   | attempt | key    | adapter |
      | input_adapter_supplied | charge | 1       | amount | pricing |
      | optional_input_absent  | charge | 1       | note   |         |
      | input_adapter_supplied | charge | 1       | caller | pricing |
      | input_adapter_supplied | charge | 1       | tax    | pricing |
      | optional_input_absent  | ship   | 1       | total  |         |

  Scenario: An input adapter that fails
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" requests input "amount" of type integer
    And step "charge" succeeds
    And the workflow declares the input adapter "pricing" for the steps "charge"
    And the input adapter "pricing" fails with "no prices" for "amount"
    When the workflow runs
    Then the journey was aborted with the abort reason "step could not be built"
    And the events include, in order:
      | event                | step   | attempt | key    | adapter | code                    |
      | input_adapter_failed | charge | 1       | amount | pricing |                         |
      | journey_aborted      | charge |         | amount | pricing | step could not be built |
    And no step ran

  Scenario: An input adapter that supplies what is not a value
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" requests input "amount" of type integer
    And step "charge" succeeds
    And the workflow declares the input adapter "pricing" for the steps "charge"
    And the input adapter "pricing" returns a non-value for "amount"
    When the workflow runs
    Then the journey was aborted

  Scenario: Data missing for an adapter that returned nothing names no adapter
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" requests input "amount" of type integer
    And step "charge" succeeds
    And the data bag is empty
    And the workflow declares the input adapter "pricing" for the steps "charge"
    When the workflow runs
    Then the journey was aborted with the abort reason "required data missing"
    And journey_aborted names no adapter

  Scenario: Every sentence about services and their providers
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
      | ship   |
    And the data bag contains:
      | key     | value  |
      | gateway | "bank" |
    And the workflow's ID generator returns "order-7"
    And step "charge" requests the service "payments" of type labeller
    And step "charge" succeeds
    And step "ship" requests the service "payments" of type labeller
    And step "ship" requests the service "labels" of type labeller
    And step "ship" succeeds
    And the workflow declares the service provider "bank" for "payments" of type labeller, once per journey
    And the service provider "bank" requests the journey ID
    And the service provider "bank" requests data from the workflow "gateway" of type string
    And the service provider "bank" requests optional data from the workflow "discount" of type string
    And the service provider "bank" labels each instance with the value it received for "gateway"
    And the service provider "bank" makes instances of second labeller
    And the service provider "bank" supplies the data {"environment": "sandbox"} with each instance
    And the workflow declares the service provider "printer" for "labels" of type labeller, for each attempt
    And the service provider "printer" serves only the steps "ship"
    And the service provider "printer" requests the step name
    And the service provider "printer" requests the attempt number
    When the workflow runs
    Then the journey succeeded
    And the service provider "bank" was called 1 times
    And the service provider "bank" received the journey ID "order-7"
    And the service provider "bank" received data from the workflow "discount" absent
    And the service provider "printer" was called 1 times
    And the service provider "printer" received the step name "ship"
    And the service provider "printer" received the attempt number 1
    And step "charge" was given the service "payments" labelled "bank" for attempt 1
    And step "ship" was given the service "payments" labelled "bank" for attempt 1
    And step "ship" was given the service "labels" labelled "printer 1" for attempt 1
    And the events include, in order:
      | event                 | step   | attempt | key      | provider | implementation  | data                       |
      | optional_input_absent | charge | 1       | discount | bank     |                 |                            |
      | service_provided      | charge | 1       | payments | bank     | second labeller | {"environment": "sandbox"} |
      | service_provided      | ship   | 1       | labels   | printer  | first labeller  |                            |

  Scenario: A provider that fails on what it receives
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And the data bag contains:
      | key     | value    |
      | gateway | "backup" |
    And step "charge" requests the service "payments" of type labeller
    And step "charge" succeeds
    And the workflow declares the service provider "bank" for "payments" of type labeller, for each attempt
    And the service provider "bank" requests data from the workflow "gateway" of type string
    And the service provider "bank" fails with "no backup" when it receives "backup" for "gateway"
    When the workflow runs
    Then the journey was aborted with the abort reason "step could not be built"
    And the events include, in order:
      | event                   | step   | attempt | key      | provider | code                    |
      | service_provider_failed | charge | 1       | payments | bank     |                         |
      | journey_aborted         | charge |         |          | bank     | step could not be built |
    And the result's abort carries the error "no backup"

  Scenario: A provider that returns nothing
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" requests the service "payments" of type labeller
    And step "charge" succeeds
    And the workflow declares the service provider "bank" for "payments" of type labeller, once per journey
    And the service provider "bank" returns nothing
    When the workflow runs
    Then the journey was aborted with the abort reason "step could not be built"
    And the service provider "bank" was called 1 times

  Scenario: A provider that fails, and one that supplies data that is not a value
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
      | ship   |
    And step "charge" requests the service "payments" of type labeller
    And step "charge" succeeds
    And step "ship" requests the service "labels" of type labeller
    And step "ship" succeeds
    And the workflow declares the service provider "bank" for "payments" of type labeller, once per journey
    And the service provider "bank" supplies data that is not a value with each instance
    And the workflow declares the service provider "printer" for "labels" of type labeller, once per journey
    And the service provider "printer" fails with "out of paper"
    When the workflow runs
    Then the journey was aborted with the abort reason "not a value"
    And the service provider "printer" was called 0 times

  Scenario: A service that fails when the step uses it
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" requests the service "payments" of type labeller
    And step "charge" succeeds
    And step "charge" does not retry abnormal terminations
    And the workflow declares the service provider "bank" for "payments" of type labeller, once per journey
    And the services the provider "bank" makes fail with "offline" when used
    When the workflow runs
    Then the journey failed
    And the events include, in order:
      | event                     | step   | attempt |
      | step_abnormal_termination | charge | 1       |
