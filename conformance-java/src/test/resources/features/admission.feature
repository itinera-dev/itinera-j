@admission
Feature: Scenarios that declare adapters, services and providers, and list the workflow

  Scenario: Two adapters with the same name, one naming a step twice
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
      | refund |
    And step "charge" succeeds
    And step "refund" succeeds
    And the workflow declares the input adapter "pricing" for the steps "charge", "charge"
    And the workflow declares the input adapter "pricing" for the steps "refund"
    When the workflow runs
    Then admission is refused with the violations:
      | violation              |
      | duplicate adapter name |
      | step adapted twice     |

  Scenario: A provider that serves one step twice, and a service of another type
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" requests the service "payments" of type labeller
    And step "charge" requests the service "readings" of type labeller
    And step "charge" succeeds
    And the workflow declares the service provider "payments" for "payments" of type labeller, for each attempt
    And the service provider "payments" serves only the steps "charge", "charge"
    And the workflow declares the service provider "meters" for "readings" of type meter, once per journey
    When the workflow runs
    Then admission is refused with the violations:
      | violation              |
      | service provided twice |
      | wrong service type     |

  Scenario: The listing of steps with adapters, services and a rescue step
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
      | ship   |
    And step "charge" has a rescue step
    And step "charge" requests the service "payments" of type labeller
    And step "charge" succeeds
    And step "charge_rescue" requests the service "payments" of type labeller
    And step "charge_rescue" succeeds
    And step "ship" succeeds
    And the workflow declares the input adapter "pricing" for the steps "charge"
    And the workflow declares the input adapter "credentials" for the steps "charge_rescue"
    And the workflow declares the service provider "gateway" for "payments" of type labeller, once per journey
    And the workflow declares the service provider "backup" for "payments" of type labeller, for each attempt
    And the service provider "backup" serves only the steps "charge_rescue"
    When the workflow is listed
    Then the listing is:
      | step   | position | policies | adapter | services          | rescue        | rescue adapter | rescue services  |
      | charge | 1        |          | pricing | payments: gateway | charge_rescue | credentials    | payments: backup |
      | ship   | 2        |          |         |                   |               |                |                  |
    And no step ran

  Scenario: A step whose attempt fails is still checked at admission
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
      | charge |
    And step "charge" attempts:
      | attempt | outcome | code     |
      | 1       | failure | declined |
    When the workflow runs
    Then admission is refused with the violations:
      | violation           |
      | duplicate step name |

  @not-run-yet
  Scenario: A workflow with an input adapter, which the runner cannot run yet
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" succeeds
    And the workflow declares the input adapter "pricing" for the steps "charge"
    When the workflow runs
    Then the journey succeeded
