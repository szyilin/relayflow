# workspace-owner-chip Specification

## Purpose

工作台列表通用所有者/负责人芯片：头像 + 真实显示名，点击打开个人名片，不单独触发行打开。

## Requirements

### Requirement: Workspace owner chip component

The workspace SHALL provide a reusable owner/user chip component that displays the member's avatar and display name, and opens the existing personal business card on click. The chip MUST be usable from document lists, drive listings, and task assignee surfaces.

#### Scenario: Chip shows avatar and name

- **WHEN** a list row renders an owner or assignee with a known `userId`
- **THEN** the UI shows an avatar (image or letter fallback) and a human display name
- **AND** MUST NOT use the bare word「我」as the only visible name for the current user (a secondary「我」badge beside the real name is allowed)

#### Scenario: Click opens business card

- **WHEN** the member clicks the owner chip
- **THEN** a peer or self `WorkspaceBusinessCard` opens (anchored popover or equivalent)
- **AND** the click MUST NOT alone open the underlying document or task row

#### Scenario: Missing avatar still clickable

- **WHEN** no avatar URL is available for the user
- **THEN** the chip still renders a letter avatar and remains clickable to open the business card

---

### Requirement: Workspace owner chip component web

The workspace web app MUST provide `WorkspaceOwnerChip` showing avatar and display name, opening `WorkspaceBusinessCard` on click without triggering the parent row open action.

#### Scenario: Chip opens card

- **WHEN** the member clicks an owner chip
- **THEN** the business card opens for that userId
- **AND** the parent list row MUST NOT open solely from that click

#### Scenario: Self name not bare 我

- **WHEN** the chip represents the current user
- **THEN** a real display name is shown
- **AND** an optional「我」badge MAY appear beside the name
