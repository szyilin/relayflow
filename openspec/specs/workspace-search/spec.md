# workspace-search Specification

## Purpose

工作台全局搜索：聚合成员、IM 会话与个人任务，经 Rail / ⌘K Modal 展示并深链跳转。

## Requirements

### Requirement: Workspace search aggregation API

The system SHALL expose `GET /app-api/infra/workspace-search` for authenticated workspace members to search across members, IM conversations, and personal tasks in a single response grouped by type.

#### Scenario: Successful grouped search

- **WHEN** an authenticated user requests `GET /app-api/infra/workspace-search?keyword=张`
- **THEN** the response includes a `groups` array with entries for `member`, `conversation`, and `task` types
- **AND** each group contains at most `limitPerGroup` items (default 5)
- **AND** all results are scoped to the JWT `tenant_id`

#### Scenario: Keyword required

- **WHEN** an authenticated user requests workspace search with an empty or whitespace-only keyword
- **THEN** the system rejects with business error `SEARCH_KEYWORD_REQUIRED`

#### Scenario: Cross-module orchestration

- **WHEN** the aggregation service executes a workspace search
- **THEN** it calls only `*-api` interfaces from `system-api`, `im-api`, and `task-api`
- **AND** does not access other modules' mappers or tables directly from `infra-biz`

### Requirement: Member keyword search

The system SHALL allow searching tenant members by nickname or mobile for workspace global search.

#### Scenario: Search member by nickname

- **WHEN** `MemberUserApi.searchMembers` is called with a keyword matching a member nickname in the tenant
- **THEN** active members matching the keyword are returned up to the requested limit
- **AND** each result includes a route suitable for opening `/app/contacts` with the member context

### Requirement: Conversation keyword search

The system SHALL allow searching IM conversations visible to the current user by conversation title or direct-chat peer display name.

#### Scenario: Search conversation by title

- **WHEN** `ImConversationApi.searchConversations` is called for the current user
- **THEN** only conversations where the user is a member are returned
- **AND** each result includes a route suitable for opening `/app/messages` with the conversation context

### Requirement: Task keyword search

The system SHALL allow searching personal tasks assigned to the current user by title.

#### Scenario: Search my task titles

- **WHEN** `TaskItemApi.searchTasks` is called for the current user
- **THEN** only tasks with `assignee_id` equal to the current user are returned
- **AND** each result includes a route suitable for opening `/app/tasks` with the task context

### Requirement: Workspace search modal UI

The workspace shell SHALL provide a search modal opened from the Rail search input and from `⌘K` / `Ctrl+K`, displaying grouped results and navigating on selection.

#### Scenario: Open search with keyboard shortcut

- **WHEN** a user presses `⌘K` or `Ctrl+K` on a workspace page
- **THEN** the search modal opens with focus on the keyword input

#### Scenario: Navigate from search result

- **WHEN** a user selects a search result with a valid in-app `route`
- **THEN** the client navigates to that route
- **AND** the target page activates the related member, conversation, or task when query parameters are present

#### Scenario: Rail search input enabled

- **WHEN** a user views the workspace Rail header
- **THEN** the search input is interactive (not permanently disabled)
- **AND** clicking it opens the same search modal
