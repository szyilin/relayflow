# workspace-owner-chip Specification

## Purpose

工作台列表通用所有者/负责人芯片：头像 + 真实显示名，点击打开个人名片，不单独触发行打开。

## Requirements

### Requirement: Workspace owner chip component

The workspace SHALL provide a reusable owner/user chip component that displays the member's avatar and display name, and opens the existing personal business card on click. The chip MUST be usable from document lists, drive listings, task assignee surfaces, **and admin console surfaces that display document owners or storage members** (e.g. admin docs file ops and storage usage).

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

#### Scenario: Admin docs reuse same chip

- **WHEN** an admin docs file or storage usage row renders an owner/member
- **THEN** the same `WorkspaceOwnerChip` component is used
- **AND** MUST NOT introduce a parallel admin-only plain-text id-under-name identity cell as the primary pattern

### Requirement: Owner chip on admin docs identity surfaces

凡管理端云文档产品设置中展示「所有者」或「成员」身份的列表/规则行，SHALL 使用 `WorkspaceOwnerChip`（头像 + 真实显示名，点击打开 `WorkspaceBusinessCard`），与工作台列表规范一致。MUST NOT 以裸 userId / objectId 作为姓名下方的主展示文案。

#### Scenario: Admin docs file owner column

- **WHEN** 管理员在 `/admin/products/docs/files` 查看含所有者的行
- **THEN** 所有者列渲染 `WorkspaceOwnerChip`
- **AND** MUST NOT 在显示名下展示裸 `ownerUserId` 作为默认副文案

#### Scenario: Chip opens business card in admin

- **WHEN** 管理员点击该 chip
- **THEN** 打开对应 self/peer `WorkspaceBusinessCard`
- **AND** MUST NOT 仅因点击 chip 触发行级主操作（如打开文档正文）

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
