# rtc-media Specification

## Purpose

平台 RTC 媒体支撑能力：自托管 LiveKit、可复用的 `RtcSessionApi`、服务端令牌与 webhook；供 meeting 与未来 IM 通话复用，不承载会议 Hub 产品语义。

## Requirements

### Requirement: RTC domain module

The system SHALL provide an `rtc` domain with Maven modules `relayflow-module-rtc-api` and `relayflow-module-rtc-biz`, persist media-session metadata in tables prefixed with `rtc_`, and load the biz module from `relayflow-server`. Cross-domain callers MUST depend only on `rtc-api` (not `rtc-biz`). The rtc domain is a platform support capability reusable by meeting and future IM; it MUST NOT own meeting Hub product semantics.

#### Scenario: Table naming

- **WHEN** a new rtc-domain table is created
- **THEN** its name starts with `rtc_`
- **AND** it includes `tenant_id BIGINT NOT NULL` for tenant-scoped rows

### Requirement: LiveKit as media plane without forking

The rtc domain SHALL integrate a self-hosted **LiveKit Server** for WebRTC media forwarding. The system MUST NOT modify LiveKit upstream source code for V1 features. API keys and secrets MUST remain server-side only. Browser clients MUST connect **directly** to LiveKit for media (using a server-issued join token); the Java application MUST NOT proxy RTP/WebRTC media.

#### Scenario: Client cannot use API secret

- **WHEN** a browser client joins a media room
- **THEN** it connects using a server-issued LiveKit JWT and configured WebSocket URL
- **AND** MUST NOT embed LiveKit API key or API secret in the frontend bundle

#### Scenario: Media path bypasses Java

- **WHEN** participants exchange audio/video
- **THEN** media flows between clients and LiveKit
- **AND** MUST NOT require the RelayFlow JVM to forward media packets

### Requirement: Reusable media session API (internal)

The `rtc-api` module SHALL expose a `RtcSessionApi` for **in-process / cross-domain Java callers only** that can create or ensure a media session, issue a join token, end a session, and query session status. V1 MUST NOT expose a public browser-facing `/app-api/rtc/join-token` endpoint. Each session MUST record a `purpose` including at least `MEETING` and reserved IM values (`IM_DIRECT`, `IM_GROUP` or equivalent), plus opaque `owner_type` + `owner_id`. rtc-biz MUST NOT interpret meeting or IM schemas.

#### Scenario: Meeting ensures session

- **WHEN** the meeting domain ensures a session with `purpose=MEETING` and owner pointing at a meeting id
- **THEN** rtc persists a `rtc_session` row and associates a LiveKit room name
- **AND** returns a session id usable for later token issuance

#### Scenario: Future IM does not require meeting module

- **WHEN** a future IM call flow needs media
- **THEN** it MUST be able to call `RtcSessionApi` with an IM-oriented `purpose` without depending on `meeting-biz` or `mtg_*` tables

### Requirement: Authorization ownership for tokens

Product domains (meeting, later im) MUST authorize the user before calling `RtcSessionApi.issueJoinToken`. `RtcSessionApi` MAY trust internal callers and MUST NOT implement meeting-membership checks itself. Join tokens MUST bind LiveKit identity to the RelayFlow user id and apply publish/subscribe grants supplied or approved by the product domain. Token TTL MUST be short-lived relative to a calendar day.

#### Scenario: Unauthorized user

- **WHEN** the meeting domain refuses join for a user
- **THEN** meeting MUST NOT call `issueJoinToken` for that user
- **AND** the client MUST NOT receive a usable LiveKit join credential

### Requirement: LiveKit server SDK in rtc-biz

The `rtc-biz` module SHALL use the official LiveKit Java/Kotlin server SDK (`io.livekit:livekit-server` or current artifact) for access-token signing and related server APIs. The dependency version MUST be managed via the repository BOM. rtc-biz MUST NOT reimplement JWT signing ad hoc unless an explicit exception is documented.

#### Scenario: Token issued via SDK

- **WHEN** `issueJoinToken` succeeds
- **THEN** the token is produced using the official server SDK with configured API key and secret

### Requirement: LiveKit webhook ingestion

The rtc domain SHALL accept LiveKit webhooks authenticated with a shared secret or signature, without requiring end-user JWT. Handlers MUST be idempotent. Webhooks MAY update `rtc_*` state only and MUST NOT write `mtg_*` or other product tables. Cross-domain side effects (if any) MUST use domain events after local commit; V1 MAY omit product consumers.

#### Scenario: Participant joined webhook

- **WHEN** LiveKit delivers a verified participant-joined webhook for a known room
- **THEN** rtc updates `rtc_*` state without failing acknowledgement on benign duplicates
- **AND** does not write meeting-domain tables directly

### Requirement: Configuration

The system SHALL read LiveKit settings from configuration (e.g. `relayflow.rtc.livekit.url`, `api-key`, `api-secret`, webhook secret). Missing required config when rtc is enabled MUST fail fast at startup or first use with a clear error.

#### Scenario: Dev config present

- **WHEN** Compose provides LiveKit and the app is configured with matching keys
- **THEN** `RtcSessionApi` can ensure a session and issue a token against that server
