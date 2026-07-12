## ADDED Requirements

### Requirement: Registration page pending invite banner

The registration page SHALL display pending enterprise invitations when a mobile number is entered, aligning with the Feishu-style "you have been invited" experience.

#### Scenario: Show invite banner on register

- **WHEN** a user enters a mobile on `/app/register` that has pending `NOT_JOINED` memberships
- **THEN** the page displays a banner listing the inviting enterprise names
- **AND** explains that registration will activate those memberships

#### Scenario: Hide banner without mobile

- **WHEN** the mobile field is empty
- **THEN** the invite banner is not shown

### Requirement: Workspace notification entry

The workspace shell SHALL provide a notification entry with unread badge for authenticated users.

#### Scenario: Notification bell with unread count

- **WHEN** an authenticated user opens the workspace shell and has unread notifications
- **THEN** the rail shows a notification control with unread count
- **AND** opening it lists recent `MEMBER_INVITE` notifications

#### Scenario: No bell when logged out

- **WHEN** the user is not authenticated
- **THEN** the workspace notification entry is not rendered
