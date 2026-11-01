@hooks
Feature: Scenarios whose policies define hooks, which read, write, end the journey and call roles

  Scenario: A step hook reads what it requested, contributes, emits and fails the journey
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" contributes "amount" = 40
    And step "charge" succeeds
    And a step policy "audit" defines the hook "on step success"
    And the hook "on step success" of policy "audit" requests step data "amount" of type integer
    And the hook "on step success" of policy "audit" requests optional step data "coupon" of type string
    And the hook "on step success" of policy "audit" requests the step name
    And the hook "on step success" of policy "audit" requests the attempt number
    And the hook "on step success" of policy "audit" requests the journey ID
    And the hook "on step success" of policy "audit" contributes "audited" = true
    And the hook "on step success" of policy "audit" emits journey_info "audited"
    And the hook "on step success" of policy "audit" returns FailWorkflow with code "fraud"
    And step "charge" has the policies "audit"
    And the workflow's ID generator returns "order-42"
    When the workflow runs
    Then the journey failed
    And the result names the step "charge" whose hook failed the journey, with the code "fraud"
    And the hook "on step success" of policy "audit" was called 1 times
    And the hook "on step success" of policy "audit" received step data "amount" = 40
    And the hook "on step success" of policy "audit" received step data "coupon" absent
    And the hook "on step success" of policy "audit" received the step name "charge"
    And the hook "on step success" of policy "audit" received the attempt number 1
    And the hook "on step success" of policy "audit" received the journey ID "order-42"
    And the contribution of "audited" is recorded as made by the hook "on step success" of policy "audit"
    And the result's data bag contains:
      | key     | value |
      | amount  | 40    |
      | audited | true  |
    And the events include, in order:
      | event          | step   | attempt | policy | hook            | lifecycle    | message | code  |
      | journey_info   | charge | 1       | audit  | on step success |              | audited |       |
      | hook_called    | charge | 1       | audit  | on step success | FailWorkflow |         | fraud |
      | journey_failed | charge |         |        |                 |              |         | fraud |

  Scenario: Failure hooks read the reason, and a workflow hook calls a role
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" allows 1 retries
    And step "charge" attempts:
      | attempt | outcome | code     | message      | details        |
      | 1       | failure | declined | card refused | {"bank": "x"} |
    And a step policy "audit" defines the hook "on step retry"
    And the hook "on step retry" of policy "audit" requests the failure reason
    And the hook "on step retry" of policy "audit" requests the retry cause
    And the hook "on step retry" of policy "audit" counts its calls and contributes the count as "retries"
    And a step policy "audit" defines the hook "on step failure"
    And the hook "on step failure" of policy "audit" requests the optional failure reason
    And the hook "on step failure" of policy "audit" requests the failure cause
    And step "charge" has the policies "audit"
    And a workflow policy "notify" defines the hook "on workflow failure"
    And the hook "on workflow failure" of policy "notify" requests data from the workflow "retries" of type integer
    And the hook "on workflow failure" of policy "notify" requests optional data from the workflow "coupon" of type string
    And the hook "on workflow failure" of policy "notify" requests the role "notifier" and calls its operation "send"
    And the workflow has the policies "notify"
    And the workflow provides the role "notifier" with the operation "send"
    When the workflow runs
    Then the journey failed
    And the hooks were called in this order:
      | policy | hook                |
      | audit  | on step retry       |
      | audit  | on step failure     |
      | notify | on workflow failure |
    And the hook "on step retry" of policy "audit" received the failure reason with code "declined", message "card refused" and details {"bank": "x"}
    And the hook "on step retry" of policy "audit" received the cause "failure"
    And the hook "on step failure" of policy "audit" received the cause "retries exhausted"
    And the hook "on workflow failure" of policy "notify" received data from the workflow "retries" = 1
    And the hook "on workflow failure" of policy "notify" received data from the workflow "coupon" absent
    And the operation "send" of the role "notifier" was called 1 times

  Scenario: A hook that throws aborts the journey, and receives the error it acts on
    Given a workflow "orders" with the steps:
      | step   |
      | charge |
    And step "charge" attempts:
      | attempt | outcome | message |
      | 1       | error   | timeout |
    And step "charge" does not retry abnormal terminations
    And a step policy "audit" defines the hook "on step abnormal termination"
    And the hook "on step abnormal termination" of policy "audit" requests the error
    And the hook "on step abnormal termination" of policy "audit" requests the optional failure reason
    And the hook "on step abnormal termination" of policy "audit" fails with "audit down"
    And a step policy "audit" defines the hook "on step failure"
    And step "charge" has the policies "audit"
    When the workflow runs
    Then the journey was aborted with the abort reason "hook failed"
    And the result's abort carries the error "audit down"
    And the hook "on step abnormal termination" of policy "audit" received an error
    And the hook "on step abnormal termination" of policy "audit" received the error with the message "timeout"
    And the hook "on step abnormal termination" of policy "audit" received no failure reason
    And the hook "on step failure" of policy "audit" was not called
