## ADDED Requirements

### Requirement: Drive folder and item persistence

The system SHALL persist personal drive folders in `doc_drive_folder` and object placements in `doc_drive_item`, and SHALL support `doc_object.type = FILE` with `storage_file_id` referencing an infra file id (logical FK; no DB FK to `infra_file`).

#### Scenario: Drive tables exist after migrate

- **WHEN** Flyway migrations for docs drive schema have been applied
- **THEN** tables `doc_drive_folder` and `doc_drive_item` exist with tenant/audit columns and soft-delete
- **AND** `doc_drive_item.object_id` is unique among non-deleted rows
- **AND** `doc_object` allows type `FILE` and stores nullable `storage_file_id`
