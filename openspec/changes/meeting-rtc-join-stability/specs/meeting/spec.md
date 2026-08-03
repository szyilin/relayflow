## ADDED Requirements

### Requirement: Stable pre-join to in-room media handoff

The meeting client MUST hand off pre-join local media intent (and, when available, already-acquired local tracks) into the in-room LiveKit session without requiring a failing second device acquisition as the only path. After join credentials are obtained, the client SHOULD pre-warm the LiveKit connection before `connect`. In-room microphone and camera toolbar indicators MUST reflect LiveKit local participant actual enabled state (not optimistic intent alone). While the client is connecting or publishing initial tracks, mic/camera toggles MUST be disabled. Participant video tiles MUST NOT flicker between video and avatar solely due to transient mute/live transitions during track startup when a camera track is present and the user intends camera on.

#### Scenario: Pre-join camera on shows local video after join

- **WHEN** the user enables the camera in pre-join and enters the room successfully
- **THEN** the local participant tile shows camera video without requiring the user to toggle camera again
- **AND** the in-room camera control indicates camera on once LiveKit reports the camera enabled

#### Scenario: Toolbar does not flap during publish

- **WHEN** the client is in the connecting or publishing phase after join
- **THEN** mic and camera toolbar buttons are disabled
- **AND** after publishing completes, toolbar icons match LiveKit local mic/camera enabled flags

### Requirement: Join path diagnostics for media connect

The meeting client MUST record a structured join timeline covering at least: join-credentials HTTP, connection pre-warm (when attempted), LiveKit `connect`, local mic/camera publish, and first local video frame (when camera was requested). A developer-facing export (query flag and/or development build) MUST allow copying a sanitized diagnostic snapshot that omits the full join token.

#### Scenario: Export join timings

- **WHEN** a developer enables RTC debug on the in-meeting page and copies diagnostics after a join attempt
- **THEN** the snapshot includes phase timings and recent media/connection events
- **AND** the snapshot does not include the full LiveKit JWT
