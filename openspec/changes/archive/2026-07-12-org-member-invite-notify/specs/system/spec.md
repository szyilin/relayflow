## ADDED Requirements

### Requirement: Member invite notification trigger

The system SHALL create an in-app notification when an administrator successfully invites a member by mobile.

#### Scenario: Invite pushes MEMBER_INVITE notify

- **WHEN** `POST /admin-api/system/user/invite` succeeds and creates or updates a `NOT_JOINED` membership
- **THEN** the system pushes a `MEMBER_INVITE` notification including tenant name and inviter display name in the payload

#### Scenario: Duplicate invite refreshes notification

- **WHEN** the same mobile already has an unread `MEMBER_INVITE` notification for the same tenant
- **THEN** the system updates the existing notification instead of creating duplicate unread rows

### Requirement: Public pending invite preview for registration

The system SHALL expose a permitAll app API so the registration page can show pending enterprise invitations for a mobile number without authentication.

#### Scenario: Preview pending tenants by mobile

- **WHEN** a client calls `GET /app-api/system/member-invite/pending?mobile={mobile}`
- **THEN** the system returns tenants where the mobile has `sys_tenant_user.status=NOT_JOINED`
- **AND** each item includes at least `tenantId` and `tenantName`
- **AND** does not expose admin-only fields such as department or role assignments

#### Scenario: No pending invites

- **WHEN** the mobile has no `NOT_JOINED` memberships
- **THEN** the system returns an empty list with `code=0`
