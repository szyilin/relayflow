## ADDED Requirements

### Requirement: Approval notification producer contract

The infra notification catalog SHALL include `APPROVAL_PENDING` as a core type usable by bpm-biz when pushing approver notifications.

#### Scenario: BPM uses approval pending type

- **WHEN** bpm-biz pushes a notification for a new approval task
- **THEN** it uses `type=APPROVAL_PENDING`
- **AND** the push succeeds without bpm-biz accessing `infra_notify` mappers directly
