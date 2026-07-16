## ADDED Requirements

### Requirement: BPM domain module

The system SHALL provide a `bpm` domain with Maven modules `relayflow-module-bpm-api` and `relayflow-module-bpm-biz`, loaded by `relayflow-server`, and persist business extension data in tables prefixed with `bpm_`.

#### Scenario: BPM module enabled in server

- **WHEN** `relayflow-server` starts with bpm enabled
- **THEN** `relayflow-module-bpm-biz` is on the classpath
- **AND** Flowable process engine is initialized

#### Scenario: BPM table naming

- **WHEN** a new bpm business extension table is created
- **THEN** its name starts with `bpm_`
- **AND** it includes `tenant_id BIGINT NOT NULL`

### Requirement: Embedded general approval process

The system SHALL ship one built-in BPMN process definition with key `general_approval` representing submit → single approver task → end.

#### Scenario: Process deployed at startup

- **WHEN** the application starts and `general_approval` is not yet deployed
- **THEN** the system deploys the built-in BPMN resource
- **AND** subsequent starts reuse the deployed definition

### Requirement: Submit approval instance

Authenticated workspace members SHALL be able to submit a general approval instance with a title and optional summary.

#### Scenario: Submit with chosen approver

- **WHEN** a user posts `POST /app-api/bpm/instance/submit` with `title` and `approverId`
- **THEN** the system starts a Flowable process instance
- **AND** creates a `bpm_process_instance_ext` row with status `RUNNING`
- **AND** assigns the approval user task to `approverId`

#### Scenario: Submit without approver uses default

- **WHEN** a user submits without `approverId`
- **THEN** the system assigns a tenant default approver (documented strategy, e.g. first super admin)
- **OR** rejects with `BPM_APPROVER_REQUIRED` if no default exists

### Requirement: Approval todo and mine lists

The system SHALL expose paginated APIs for approvals the user must act on and approvals the user submitted.

#### Scenario: Todo page

- **WHEN** a user requests `GET /app-api/bpm/task/todo/page`
- **THEN** the system returns pending Flowable tasks assigned to the current user within the tenant

#### Scenario: My submissions page

- **WHEN** a user requests `GET /app-api/bpm/instance/mine/page`
- **THEN** the system returns `bpm_process_instance_ext` rows where `applicant_id` is the current user

### Requirement: Approve or reject task

Assignees SHALL be able to complete pending approval tasks with approve or reject actions.

#### Scenario: Approve task

- **WHEN** the assignee posts `POST /app-api/bpm/task/approve` for an owned pending task
- **THEN** the Flowable task completes with an approve outcome
- **AND** the extension record status becomes `APPROVED`

#### Scenario: Reject task

- **WHEN** the assignee posts `POST /app-api/bpm/task/reject` for an owned pending task
- **THEN** the Flowable task completes with a reject outcome
- **AND** the extension record status becomes `REJECTED`

#### Scenario: Forbidden on others task

- **WHEN** a user attempts to approve a task not assigned to them
- **THEN** the system rejects with `BPM_TASK_FORBIDDEN`

### Requirement: Approval pending bot delivery

When a new approval task is created for an approver, bpm-biz SHALL call `ImBotApi.send` (via `im-api` only) with bot code `approval-bot` and target scope `SINGLE` for the approver. The module MUST NOT call `NotifyInboxApi` or access `infra_notify` / `im_*` mappers. `ImBotApi.send` failures MUST NOT fail approval submit APIs (best-effort reach).

#### Scenario: Notify approver on new task via bot_dm

- **WHEN** a submitted instance creates a pending approval task for user B in tenant T
- **THEN** bpm-biz calls `ImBotApi.send` with `botCode=approval-bot` and target SINGLE `{ tenantId: T, userId: B }`
- **AND** uses a stable `dedupeKey` such as `APPROVAL_PENDING:{extId}`
- **AND** deep link metadata includes a `route` under `/app/approvals` with `entityType=approval`
- **AND** bpm-biz does not depend on `infra-biz` or deleted notify inbox APIs

#### Scenario: Delivery failure does not block submit

- **WHEN** `ImBotApi.send` throws or returns a delivery failure
- **THEN** the approval submit API still succeeds
- **AND** the failure is logged server-side

### Requirement: Workspace approvals page

The `/app/approvals` page SHALL integrate with bpm APIs via store layer and expose todo and my-submissions views.

#### Scenario: View pending approvals

- **WHEN** a user opens `/app/approvals`
- **THEN** pending items assigned to the user are listed from the API
- **AND** the user can open detail and approve or reject

#### Scenario: Workspace navigation entry

- **WHEN** a user views the workspace Rail
- **THEN** an approvals navigation item is available routing to `/app/approvals`
