## ADDED Requirements

### Requirement: Infra file slice integration complete

The infra file management vertical slice SHALL be fully integrated end-to-end without frontend mocks for file list, upload, delete, or admin download.

#### Scenario: Admin file page end-to-end

- **WHEN** an admin uses `/admin/infra/file` with valid permissions
- **THEN** the page loads files from the real API, supports presigned upload, download via 302 redirect, and logical delete
